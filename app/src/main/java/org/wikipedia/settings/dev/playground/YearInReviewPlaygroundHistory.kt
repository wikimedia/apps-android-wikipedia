package org.wikipedia.settings.dev.playground

import androidx.room.withTransaction
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.yearinreview.data.PrefsYearInReviewStore
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import org.wikipedia.yearinreview.presentation.YearInReviewFlowDecider
import org.wikipedia.yearinreview.presentation.YearInReviewPage
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

// withExcludedPages adds main pages and non-article pages, which the articles read count must skip
data class YearInReviewPlaygroundHistory(val articles: Int, val visitedDays: Int, val peakMonthDays: Int, val withExcludedPages: Boolean = false)

data class YearInReviewPlaygroundTestEntries(val summary: String, val expectedArticlesRead: Int)

data class YearInReviewPlaygroundHistoryState(
    val dateRange: String,
    val selectedPreset: YearInReviewPlaygroundHistoryPreset? = null,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val flowSummary: String? = null,
    val testEntries: YearInReviewPlaygroundTestEntries? = null,
    val warning: String? = null
)

// The smallest history that reaches each reading slide state in YearInReviewReadingStats
// warning explains why the state wasn't reached, which happens when real history adds to the test entries
enum class YearInReviewPlaygroundHistoryPreset(
    val label: String,
    val description: String,
    val history: YearInReviewPlaygroundHistory,
    val warning: (YearInReviewReadingStats) -> String? = { null }
) {
    VISITS_WITHOUT_PEAK_MONTH(
        "Visits, no peak month",
        "Visits slide is shown without the peak month subtext, since no month has 2 or more visited days.",
        YearInReviewPlaygroundHistory(YearInReviewReadingStats.MIN_ARTICLES_READ, YearInReviewReadingStats.MIN_VISITED_DAYS,
            YearInReviewReadingStats.MIN_PEAK_MONTH_VISITED_DAYS - 1),
        warning = { stats ->
            if (stats.hasPeakMonth) {
                "Not reached: your real history has ${stats.peakMonthVisitedDays} visited days in " +
                        "${Month.of(stats.peakMonth).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())}, so the peak month subtext still shows. " +
                        "To test this, clear your history from the History tab, then apply this preset again."
            } else {
                null
            }
        }
    ),
    VISITS_WITH_PEAK_MONTH(
        "Visits, peak month",
        "Visits slide is shown with the peak month subtext.",
        YearInReviewPlaygroundHistory(YearInReviewReadingStats.MIN_ARTICLES_READ,
            maxOf(YearInReviewReadingStats.MIN_VISITED_DAYS, YearInReviewReadingStats.MIN_PEAK_MONTH_VISITED_DAYS),
            YearInReviewReadingStats.MIN_PEAK_MONTH_VISITED_DAYS)
    ),
    ARTICLES_READ_MIXED_PAGES(
        "Articles read, mixed pages",
        "Adds articles, the main page of each app language, and Talk, User, Wikipedia and File pages. Only the articles count.",
        YearInReviewPlaygroundHistory(articles = 5, visitedDays = YearInReviewReadingStats.MIN_VISITED_DAYS,
            peakMonthDays = YearInReviewReadingStats.MIN_PEAK_MONTH_VISITED_DAYS, withExcludedPages = true)
    )
}

object YearInReviewPlaygroundHistorySeeder {
    private const val TITLE_PREFIX = "YiRPlayground"
    private const val DISPLAY_TITLE_PREFIX = "YiR playground"
    private val NON_ARTICLE_NAMESPACES = listOf("Talk", "User", "Wikipedia", "File")

    val dateRange get() = YearInReviewConfig.insightsDateRange(YearInReviewConfig.cachedRemoteConfig)

    private fun entries(history: YearInReviewPlaygroundHistory): List<HistoryEntry> {
        val range = dateRange
        val datesByMonth = generateSequence(range.start) { it.plusDays(1) }
            .takeWhile { it <= range.endInclusive }
            .groupBy { YearMonth.from(it) }
        val peakMonth = datesByMonth.maxBy { it.value.size }
        val otherDates = (datesByMonth - peakMonth.key).values.flatMap {
            it.take((history.peakMonthDays - 1).coerceAtLeast(1))
        }
        val dates = peakMonth.value.take(history.peakMonthDays) + otherDates.take(history.visitedDays - history.peakMonthDays)
        check(dates.size == history.visitedDays) { "The preset doesn't fit the configured date range." }
        val wikiSite = WikipediaApp.instance.wikiSite
        return (0 until maxOf(history.articles, dates.size)).map { index ->
            val article = index % history.articles + 1
            HistoryEntry(
                authority = wikiSite.authority(),
                lang = wikiSite.languageCode,
                apiTitle = "${TITLE_PREFIX}_$article",
                displayTitle = "$DISPLAY_TITLE_PREFIX article $article",
                timestamp = Date.from(dates[index % dates.size].atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()),
                source = HistoryEntry.SOURCE_SEARCH
            )
        }
    }

    // Main pages are added as search visits, so only their title excludes them.
    private fun excludedEntries(timestamp: Date): Pair<List<HistoryEntry>, List<HistoryEntry>> {
        val mainPages = WikipediaApp.instance.languageState.appLanguageCodes.map { lang ->
            HistoryEntry(PageTitle(MainPageNameData.valueFor(lang), WikiSite.forLanguageCode(lang)), HistoryEntry.SOURCE_SEARCH, timestamp).apply {
                displayTitle = "$DISPLAY_TITLE_PREFIX main page ($lang: ${MainPageNameData.valueFor(lang)})"
            }
        }
        val wikiSite = WikipediaApp.instance.wikiSite
        val nonArticles = NON_ARTICLE_NAMESPACES.map { namespace ->
            HistoryEntry(
                authority = wikiSite.authority(),
                lang = wikiSite.languageCode,
                apiTitle = "${TITLE_PREFIX}_$namespace",
                displayTitle = "$DISPLAY_TITLE_PREFIX $namespace page",
                namespace = namespace,
                timestamp = timestamp,
                source = HistoryEntry.SOURCE_SEARCH
            )
        }
        return mainPages to nonArticles
    }

    // Null clears only test entries; replacement is a single database transaction.
    suspend fun update(history: YearInReviewPlaygroundHistory?): YearInReviewPlaygroundTestEntries? {
        val articles = history?.let { entries(it) }.orEmpty()
        val (mainPages, nonArticles) = if (history?.withExcludedPages == true) excludedEntries(articles.first().timestamp) else emptyList<HistoryEntry>() to emptyList()
        return AppDatabase.instance.withTransaction {
            val dao = AppDatabase.instance.historyEntryDao()
            dao.deleteByDisplayTitlePrefix(DISPLAY_TITLE_PREFIX)
            val realArticlesRead = YearInReviewRepositoryImpl().getReadingStats(dateRange).articlesReadCount
            dao.insert(articles + mainPages + nonArticles)
            PrefsYearInReviewStore.onReadingHistoryCleared()
            history?.takeIf { it.withExcludedPages }?.let {
                YearInReviewPlaygroundTestEntries(
                    summary = "Test entries added: ${articles.size + mainPages.size + nonArticles.size} (see the History tab)\n" +
                            "Articles, counted: ${it.articles}\n" +
                            "Main pages, skipped: ${mainPages.joinToString { page -> page.displayTitle.removePrefix("$DISPLAY_TITLE_PREFIX main page (").removeSuffix(")") }}\n" +
                            "Non-article pages, skipped: ${NON_ARTICLE_NAMESPACES.joinToString()}\n" +
                            "Expected articles read: ${realArticlesRead + it.articles} (${it.articles} test + $realArticlesRead from your real history)",
                    expectedArticlesRead = realArticlesRead + it.articles
                )
            }
        }
    }

    // Mirrors the repository: cached reading stats win until the history changes
    suspend fun withReadingStats(state: YearInReviewPlaygroundHistoryState): YearInReviewPlaygroundHistoryState {
        val range = dateRange
        val currentStats = YearInReviewRepositoryImpl().getReadingStats(range)
        val readingStats = PrefsYearInReviewStore.get(YearInReviewConfig.YEAR)?.readingStats ?: currentStats
        val dao = AppDatabase.instance.historyEntryDao()
        val visitsCount = dao.getEntriesCountBetween(range.startMillis, range.endMillis)
        val pagesCount = dao.getDistinctEntriesCountBetween(range.startMillis, range.endMillis)
        val historyBreakdown = "\n\nYour history in this date range\n" +
                "Total visits: $visitsCount\n" +
                "Unique pages: $pagesCount\n" +
                "Unique articles, counted: ${currentStats.articlesReadCount}\n" +
                "Skipped (main pages, non-article pages): ${pagesCount - currentStats.articlesReadCount}"
        val snapshot = YearInReviewSnapshot(
            year = YearInReviewConfig.YEAR,
            isLoggedIn = AccountUtil.isLoggedIn,
            isDonationEligible = false,
            remoteConfig = YearInReviewConfig.cachedRemoteConfig ?: YearInReviewConfig.fallbackConfig,
            readingStats = readingStats,
            rewardData = YearInReviewRewardData(isDonor = false, isEditor = false)
        )
        val reasons = listOfNotNull(
            "not logged in".takeIf { !snapshot.isLoggedIn },
            "not enough personalized slides".takeIf { !YearInReviewFlowDecider.isDataRich(snapshot) }
        )
        val flow = if (reasons.isEmpty()) "Personalized" else "Collective (${reasons.joinToString()})"
        val articlesReadSlide = YearInReviewFlowDecider.pages(snapshot).filterIsInstance<YearInReviewPage.ArticlesRead>().firstOrNull()?.let {
            when {
                it.isEmptyState -> "\nArticles read slide: empty state"
                it.percentile.isNotEmpty() -> "\nArticles read slide: ${it.count}, with the top ${it.percentile}% text"
                else -> "\nArticles read slide: ${it.count}, with the general text"
            }
        }.orEmpty()
        val articlesReadMismatch = state.testEntries?.takeIf { it.expectedArticlesRead != readingStats.articlesReadCount }?.let {
            "Unexpected: ${readingStats.articlesReadCount} articles read, but ${it.expectedArticlesRead} expected. Main pages or non-article pages may be counted."
        }
        return state.copy(
            flowSummary = "Flow: $flow\n" +
                    "Articles read: ${readingStats.articlesReadCount}\n" +
                    "Visited days: ${readingStats.visitedDaysCount}\n" +
                    "Visited days in the peak month: ${readingStats.peakMonthVisitedDays}" +
                    articlesReadSlide + historyBreakdown,
            warning = articlesReadMismatch ?: state.selectedPreset?.let {
                if (reasons.isEmpty()) it.warning(readingStats) else "Not reached: Year in Review shows the collective flow (${reasons.joinToString()})."
            }
        )
    }
}
