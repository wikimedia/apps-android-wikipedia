package org.wikipedia.database

import android.app.Instrumentation
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import java.io.File
import java.time.LocalDateTime
import java.util.Date
import kotlin.random.Random
import kotlin.system.measureNanoTime

// Compares HistoryEntryWithImageDao.findEntriesVisitedMultipleTimesBetween (excludes only the primary language's Main Page)
// with findEntriesVisitedMultipleTimesExcludingMainPagesBetween (excludes the Main Pages of all app languages),
// against seeded history of increasing size. Saves a markdown report to Downloads and prints it in the instrumentation output.
@RunWith(AndroidJUnit4::class)
class RevisitedArticlesQueryBenchmark {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private data class RevisitedArticle(val lang: String, val apiTitle: String, val visitCount: Int)

    private class Measurement(val label: String, val timesMs: List<Double>, val results: List<RevisitedArticle>) {
        val median get() = timesMs[timesMs.size / 2]
        val p90 get() = timesMs[(timesMs.size * 9) / 10]
        val mainPagesInResults get() = results.filter { !it.apiTitle.startsWith(ARTICLE_PREFIX) }
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

            val measurements = OLD_QUERY_PRIMARY_LANGS.map { primaryLang ->
                measure("Old, primary $primaryLang") { runOldQuery(primaryLang) }
            } + measure("New, all app languages") { runNewQuery() }

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
            assertTrue("Main Pages in results at $rowCount rows: ${newQuery.mainPagesInResults}", newQuery.mainPagesInResults.isEmpty())
        }

        writeReport(timingRows, resultRows)
    }

    private suspend fun measure(label: String, query: suspend () -> List<RevisitedArticle>): Measurement {
        repeat(WARMUP_RUNS) { query() }
        var results = emptyList<RevisitedArticle>()
        val timesMs = (1..MEASURED_RUNS).map {
            measureNanoTime { results = query() } / 1_000_000.0
        }.sorted()
        return Measurement(label, timesMs, results)
    }

    private suspend fun runOldQuery(primaryLang: String): List<RevisitedArticle> {
        return db.historyEntryWithImageDao().findEntriesVisitedMultipleTimesBetween(YEAR_START, YEAR_END, primaryLang, LIMIT)
            .map { (entry, visitCount) -> RevisitedArticle(entry.lang, entry.apiTitle, visitCount) }
    }

    private suspend fun runNewQuery(): List<RevisitedArticle> {
        return db.historyEntryWithImageDao().findEntriesVisitedMultipleTimesExcludingMainPagesBetween(YEAR_START, YEAR_END, APP_LANGUAGES, LIMIT)
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
            }
        }
    }

    private fun writeReport(timingRows: List<String>, resultRows: List<String>) {
        val report = buildString {
            appendLine("# Revisited articles query benchmark")
            appendLine()
            appendLine("- Run: ${LocalDateTime.now().withNano(0)} on ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- History: visits spread over 2024–2025, queried for 2025. Languages ${SEED_LANGS.distinct().joinToString()} (en is 3× as common), " +
                    "${"%,d".format(DISTINCT_TITLES)} distinct articles per language, $MAIN_PAGE_PERCENT% Main Page visits, $TALK_PAGE_PERCENT% Talk page visits.")
            appendLine("- App languages: ${APP_LANGUAGES.joinToString { "$it (${MainPageNameData.valueFor(it)})" }}")
            appendLine("- Old: `findEntriesVisitedMultipleTimesBetween`, which excludes only the primary language's Main Page.")
            appendLine("- New: `findEntriesVisitedMultipleTimesExcludingMainPagesBetween`, which excludes the Main Pages of all app languages.")
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

        val reportLocation = saveReport(report)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString(Instrumentation.REPORT_KEY_STREAMRESULT, "\n$report\nReport saved to $reportLocation\n")
        })
    }

    // Saves to the public Downloads folder, so the report shows up in the Files app and survives uninstalling the app.
    private fun saveReport(report: String): String {
        val fileName = "revisited-articles-benchmark-${LocalDateTime.now().withNano(0).toString().replace(':', '-')}.md"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val reportFile = File(context.getExternalFilesDir(null), fileName)
            reportFile.writeText(report)
            return reportFile.absolutePath
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(report.toByteArray()) }
        return "/sdcard/${Environment.DIRECTORY_DOWNLOADS}/$fileName"
    }

    companion object {
        private const val DB_NAME = "revisited-articles-benchmark.db"
        private const val ARTICLE_PREFIX = "Article_"
        private val ROW_COUNTS = listOf(5_000, 50_000, 200_000)
        private const val DISTINCT_TITLES = 8_000
        private const val MAIN_PAGE_PERCENT = 3
        private const val TALK_PAGE_PERCENT = 5
        private val SEED_LANGS = listOf("en", "en", "en", "de", "ja", "fr", "es")
        private val APP_LANGUAGES = SEED_LANGS.distinct()
        private val OLD_QUERY_PRIMARY_LANGS = listOf("en", "de")
        private const val WARMUP_RUNS = 5
        private const val MEASURED_RUNS = 20
        private const val LIMIT = 10

        // 2024-01-01 .. 2026-01-01 UTC, queried for 2025.
        private const val SEED_START = 1704067200000L
        private const val SEED_END = 1767225600000L
        private const val YEAR_START = 1735689600000L
        private const val YEAR_END = 1767225599999L
    }
}
