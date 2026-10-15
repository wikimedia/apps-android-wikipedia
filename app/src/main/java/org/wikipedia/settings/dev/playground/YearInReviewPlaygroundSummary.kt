package org.wikipedia.settings.dev.playground

import org.wikipedia.auth.AccountUtil
import org.wikipedia.yearinreview.data.PrefsYearInReviewStore
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import org.wikipedia.yearinreview.presentation.YearInReviewFlowDecider
import org.wikipedia.yearinreview.presentation.YearInReviewPage

object YearInReviewPlaygroundSummary {

    // What Year in Review shows with real data. Mirrors the repository: cached reading stats win until the history changes.
    suspend fun realData(): String {
        val dateRange = YearInReviewConfig.insightsDateRange(YearInReviewConfig.cachedRemoteConfig)
        val repository = YearInReviewRepositoryImpl()
        val readingStats = PrefsYearInReviewStore.get(YearInReviewConfig.YEAR)?.readingStats ?: repository.getReadingStats(dateRange)
        val snapshot = YearInReviewSnapshot(
            year = YearInReviewConfig.YEAR,
            isLoggedIn = AccountUtil.isLoggedIn,
            isDonationEligible = false,
            remoteConfig = YearInReviewConfig.cachedRemoteConfig ?: YearInReviewConfig.fallbackConfig,
            readingStats = readingStats,
            rewardData = YearInReviewRewardData(isDonor = false, isEditor = false)
        )
        val entryVersion = if (repository.getEntryStats().isDataRich) "Data-rich" else "Low-data"
        val reasons = listOfNotNull(
            "not logged in".takeIf { !snapshot.isLoggedIn },
            "not enough personalized slides".takeIf { !YearInReviewFlowDecider.hasEnoughPersonalizedPages(snapshot) }
        )
        val slides = if (reasons.isEmpty()) "Personalized" else "Collective (${reasons.joinToString()})"
        val articlesReadSlide = YearInReviewFlowDecider.pages(snapshot).filterIsInstance<YearInReviewPage.ArticlesRead>().firstOrNull()?.let {
            when {
                it.isEmptyState -> "\nArticles read slide: empty state"
                it.percentile.isNotEmpty() -> "\nArticles read slide: ${it.count}, with the top ${it.percentile}% text"
                else -> "\nArticles read slide: ${it.count}, with the general text"
            }
        }.orEmpty()
        return "Entry card and announcement: $entryVersion\n" +
                "Slides: $slides\n" +
                "Reading history from ${dateRange.start} to ${dateRange.endInclusive}\n" +
                "Articles read: ${readingStats.articlesReadCount}\n" +
                "Visited days: ${readingStats.visitedDaysCount}\n" +
                "Visited days in the peak month: ${readingStats.peakMonthVisitedDays}" +
                articlesReadSlide
    }
}
