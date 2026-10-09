package org.wikipedia.settings.dev.playground

import androidx.room.withTransaction
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.HistoryEntry
import org.wikipedia.yearinreview.data.PrefsYearInReviewStore
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import org.wikipedia.yearinreview.presentation.YearInReviewFlowDecider
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

data class YearInReviewPlaygroundHistory(val articles: Int, val visitedDays: Int, val peakMonthDays: Int)

data class YearInReviewPlaygroundHistoryState(
    val dateRange: String,
    val selectedPreset: YearInReviewPlaygroundHistoryPreset? = null,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val flowSummary: String? = null,
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
    )
}

object YearInReviewPlaygroundHistorySeeder {
    private const val TITLE_PREFIX = "YiRPlayground"

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
                displayTitle = "YiR playground article $article",
                timestamp = Date.from(dates[index % dates.size].atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()),
                source = HistoryEntry.SOURCE_SEARCH
            )
        }
    }

    // Null clears only test entries; replacement is a single database transaction.
    suspend fun update(history: YearInReviewPlaygroundHistory?) {
        val entries = history?.let { entries(it) }.orEmpty()
        AppDatabase.instance.withTransaction {
            val dao = AppDatabase.instance.historyEntryDao()
            dao.deleteByApiTitlePrefix(TITLE_PREFIX)
            dao.insert(entries)
            PrefsYearInReviewStore.onReadingHistoryCleared()
        }
    }

    // Mirrors the repository: cached reading stats win until the history changes
    suspend fun withReadingStats(state: YearInReviewPlaygroundHistoryState): YearInReviewPlaygroundHistoryState {
        val readingStats = PrefsYearInReviewStore.get(YearInReviewConfig.YEAR)?.readingStats
            ?: YearInReviewRepositoryImpl().getReadingStats(dateRange)
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
        val flow = if (reasons.isEmpty()) "Personalized flow" else "Collective flow: ${reasons.joinToString()}"
        return state.copy(
            flowSummary = "$flow\n${readingStats.articlesReadCount} articles read · ${readingStats.visitedDaysCount} visited days · " +
                    "${readingStats.peakMonthVisitedDays} days in the peak month",
            warning = state.selectedPreset?.let {
                if (reasons.isEmpty()) it.warning(readingStats) else "Not reached: Year in Review shows the collective flow (${reasons.joinToString()})."
            }
        )
    }
}
