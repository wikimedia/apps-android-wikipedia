package org.wikipedia.database

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.staticdata.MainPageNameData
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.system.measureNanoTime

// Compares HistoryEntryWithImageDao.findEntriesVisitedMultipleTimesBetween (excludes only the primary language's Main Page)
// with findEntriesVisitedMultipleTimesExcludingMainPagesBetween (excludes the Main Pages of all app languages),
// against seeded history of increasing size. Logs a markdown report to logcat under the "RevisitedArticlesBenchmark" tag.
@RunWith(AndroidJUnit4::class)
class RevisitedArticlesQueryBenchmark {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private data class RevisitedArticle(val lang: String, val apiTitle: String, val visitCount: Int)

    private class Measurement(val label: String, val timesMs: List<Double>, val results: List<RevisitedArticle>) {
        val median get() = (timesMs[(timesMs.size - 1) / 2] + timesMs[timesMs.size / 2]) / 2
        val p90 get() = timesMs[ceil(timesMs.size * 0.9).toInt() - 1]
        val mainPagesInResults get() = results.filter { (it.lang to it.apiTitle) in MAIN_PAGE_KEYS }
    }

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
    }

    @After
    fun closeDb() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun benchmarkOldAndNewRevisitedArticlesQueries() = runBlocking {
        val timingRows = mutableListOf<String>()
        val resultRows = mutableListOf<String>()
        var seededRows = 0
        ROW_COUNTS.forEach { rowCount ->
            seedHistory(rowCount - seededRows, seed = rowCount)
            seededRows = rowCount
            val rowsInYear = countRowsInYear()

            val queries = OLD_QUERY_PRIMARY_LANGS.map { primaryLang ->
                QueryCase("Old, primary $primaryLang") { runOldQuery(primaryLang) }
            } + listOf(1, 3, APP_LANGUAGES.size).map { languageCount ->
                QueryCase("New, $languageCount app languages") { runNewQuery(APP_LANGUAGES.take(languageCount)) }
            }
            val measurements = measure(queries)

            val baselineMedian = measurements.first().median
            measurements.forEach { measurement ->
                val change = if (measurement === measurements.first()) "baseline" else "%+.0f%%".format((measurement.median / baselineMedian - 1) * 100)
                val times = listOf(measurement.timesMs.first(), measurement.median, measurement.p90, measurement.timesMs.last())
                    .joinToString(" | ") { "%.2f".format(it) }
                timingRows += "| ${"%,d".format(rowCount)} | ${"%,d".format(rowsInYear)} | ${measurement.label} | $times | $change |"
                val mainPages = measurement.mainPagesInResults
                    .joinToString { "${it.lang}:${it.apiTitle} (rank ${measurement.results.indexOf(it) + 1}, ${it.visitCount} visits)" }
                    .ifEmpty { "none" }
                resultRows += "| ${"%,d".format(rowCount)} | ${measurement.label} | ${measurement.results.size} | $mainPages |"
            }

            val newQuery = measurements.last()
            assertTrue("Expected revisited articles at $rowCount rows", newQuery.results.isNotEmpty())
            assertTrue("Main Pages in results at $rowCount rows: ${newQuery.mainPagesInResults}", newQuery.mainPagesInResults.isEmpty())
        }

        writeReport(timingRows, resultRows)
    }

    private class QueryCase(val label: String, val run: suspend () -> List<RevisitedArticle>)

    private suspend fun measure(queries: List<QueryCase>): List<Measurement> {
        suspend fun runRound(round: Int, action: suspend (Int) -> Unit) {
            queries.indices.forEach { offset -> action((round + offset) % queries.size) }
        }
        repeat(WARMUP_RUNS) { round -> runRound(round) { queries[it].run() } }
        val times = List(queries.size) { mutableListOf<Double>() }
        val results = MutableList(queries.size) { emptyList<RevisitedArticle>() }
        repeat(MEASURED_RUNS) { round ->
            runRound(round) { index ->
                times[index] += measureNanoTime { results[index] = queries[index].run() } / 1_000_000.0
            }
        }
        return queries.mapIndexed { index, query -> Measurement(query.label, times[index].sorted(), results[index]) }
    }

    private suspend fun runOldQuery(primaryLang: String): List<RevisitedArticle> {
        return db.historyEntryWithImageDao().findEntriesVisitedMultipleTimesBetween(YEAR_START, YEAR_END, primaryLang, LIMIT)
            .map { (entry, visitCount) -> RevisitedArticle(entry.lang, entry.apiTitle, visitCount) }
    }

    private suspend fun runNewQuery(languages: List<String>): List<RevisitedArticle> {
        return db.historyEntryWithImageDao().findEntriesVisitedMultipleTimesExcludingMainPagesBetween(YEAR_START, YEAR_END, languages, LIMIT)
            .map { (entry, visitCount) -> RevisitedArticle(entry.lang, entry.apiTitle, visitCount) }
    }

    private fun countRowsInYear(): Int {
        val query = SimpleSQLiteQuery("SELECT COUNT(*) FROM HistoryEntry WHERE timestamp BETWEEN ? AND ?", arrayOf<Any>(YEAR_START, YEAR_END))
        return db.openHelper.readableDatabase.query(query).use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    private suspend fun seedHistory(count: Int, seed: Int) {
        val random = Random(seed)
        val historyDao = db.historyEntryDao()
        db.withTransaction {
            repeat(count) {
                val lang = SEED_LANGS[random.nextInt(SEED_LANGS.size)]
                val roll = random.nextInt(100)
                val timestamp = Date(SEED_START + random.nextLong(SEED_END - SEED_START))
                val entry = if (roll < MAIN_PAGE_PERCENT) {
                    HistoryEntry(PageTitle(MainPageNameData.valueFor(lang), WikiSite.forLanguageCode(lang)), HistoryEntry.SOURCE_MAIN_PAGE, timestamp)
                } else {
                    HistoryEntry(
                        authority = "$lang.wikipedia.org",
                        lang = lang,
                        apiTitle = "$ARTICLE_PREFIX${random.nextInt(DISTINCT_TITLES)}",
                        displayTitle = "Article",
                        namespace = if (roll < MAIN_PAGE_PERCENT + TALK_PAGE_PERCENT) "Talk" else "",
                        timestamp = timestamp
                    )
                }
                historyDao.insertEntry(entry)
                if (entry.apiTitle.hashCode() % 2 == 0) {
                    db.pageImagesDao().insertPageImage(PageImage(entry.lang, entry.namespace, entry.apiTitle,
                        "${entry.apiTitle}.jpg", "Description of ${entry.apiTitle}", timeSpentSec = 30))
                }
            }
        }
    }

    private fun writeReport(timingRows: List<String>, resultRows: List<String>) {
        val report = buildString {
            appendLine("# Revisited articles warm DAO-call benchmark")
            appendLine()
            appendLine("- Run: ${LocalDateTime.now().withNano(0)} on ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- History: visits spread over 2024–2025, queried for 2025. Languages ${SEED_LANGS.distinct().joinToString()} (en is 3× as common), " +
                    "${"%,d".format(DISTINCT_TITLES)} possible article titles per language, $MAIN_PAGE_PERCENT% Main Page visits, $TALK_PAGE_PERCENT% Talk page visits.")
            appendLine("- App languages: ${APP_LANGUAGES.joinToString { "$it (${MainPageNameData.valueFor(it)})" }}")
            appendLine("- Old: `findEntriesVisitedMultipleTimesBetween`, which excludes only the primary language's Main Page.")
            appendLine("- New: `findEntriesVisitedMultipleTimesExcludingMainPagesBetween`, which excludes Main Pages for 1, 3, or all 5 app languages.")
            appendLine("- Image metadata is seeded for approximately half of article keys; the join includes both matches and misses.")
            appendLine("- Measures warm DAO-call latency, including coroutine scheduling, key construction, Room materialization, and result mapping; not cold-start latency or isolated SQL execution.")
            appendLine("- All variants warm up before measurement; execution order rotates each round. Median averages the middle pair; p90 uses nearest rank and is only a rough tail estimate.")
            appendLine("- Each query: $WARMUP_RUNS warmup runs, then $MEASURED_RUNS timed runs, limit $LIMIT. Times in milliseconds.")
            appendLine()
            appendLine("## Timings")
            appendLine()
            appendLine("| History rows | Rows in 2025 | Query | Min | Median | p90 | Max | Median vs old (primary ${OLD_QUERY_PRIMARY_LANGS.first()}) |")
            appendLine("|---:|---:|---|---:|---:|---:|---:|---:|")
            timingRows.forEach { appendLine(it) }
            appendLine()
            appendLine("## Main Pages in the results")
            appendLine()
            appendLine("| History rows | Query | Results | Main Pages among them |")
            appendLine("|---:|---|---:|---|")
            resultRows.forEach { appendLine(it) }
        }

        // Logcat truncates entries over ~4 KB, so a longer report is split at line breaks.
        val entries = mutableListOf(StringBuilder())
        report.lineSequence().forEach { line ->
            if (entries.last().toString().toByteArray().size + line.toByteArray().size >= LOGCAT_ENTRY_MAX_BYTES) {
                entries += StringBuilder()
            }
            entries.last().appendLine(line)
        }
        entries.forEach { Log.i(TAG, "\n$it") }
    }

    companion object {
        private const val TAG = "RevisitedArticlesBenchmark"
        private const val LOGCAT_ENTRY_MAX_BYTES = 3_500
        private const val DB_NAME = "revisited-articles-benchmark.db"
        private const val ARTICLE_PREFIX = "Article_"
        private val ROW_COUNTS = listOf(5_000, 50_000, 200_000)
        private const val DISTINCT_TITLES = 8_000
        private const val MAIN_PAGE_PERCENT = 3
        private const val TALK_PAGE_PERCENT = 5
        private val SEED_LANGS = listOf("en", "en", "en", "de", "ja", "fr", "es")
        private val APP_LANGUAGES = SEED_LANGS.distinct()
        private val MAIN_PAGE_KEYS = APP_LANGUAGES.map { lang ->
            lang to PageTitle(MainPageNameData.valueFor(lang), WikiSite.forLanguageCode(lang)).text
        }.toSet()
        private val OLD_QUERY_PRIMARY_LANGS = listOf("en", "de")
        private const val WARMUP_RUNS = 5
        private const val MEASURED_RUNS = 50
        private const val LIMIT = 10

        // 2024-01-01 .. 2026-01-01 UTC, queried for 2025.
        private const val SEED_START = 1704067200000L
        private const val SEED_END = 1767225600000L
        private const val YEAR_START = 1735689600000L
        private const val YEAR_END = 1767225599999L
    }
}
