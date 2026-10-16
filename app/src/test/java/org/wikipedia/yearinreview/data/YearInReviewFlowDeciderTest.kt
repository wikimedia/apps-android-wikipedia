package org.wikipedia.yearinreview.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.yearinreview.presentation.YearInReviewCollectiveInsight
import org.wikipedia.yearinreview.presentation.YearInReviewFlowDecider
import org.wikipedia.yearinreview.presentation.YearInReviewPage

class YearInReviewFlowDeciderTest {

    private val twoPersonalizedPagesStats = YearInReviewReadingStats(articlesReadCount = 3, visitedDaysCount = 2, peakMonth = 1, peakMonthVisitedDays = 2)
    private val onePersonalizedPageStats = YearInReviewReadingStats(articlesReadCount = 3, visitedDaysCount = 1, peakMonth = 1, peakMonthVisitedDays = 1)
    private val contributorRewardData = YearInReviewRewardData(isDonor = true, isEditor = true)
    private val collectiveSlides = YearInReviewCollectiveInsight.entries.map { YearInReviewPage.Collective(it, YearInReviewConfig.fallbackConfig) }
    private val topReadPercentagesConfig = RemoteConfig.RemoteConfigYearInReview(
        year = YearInReviewConfig.YEAR,
        topReadPercentages = listOf(
            RemoteConfig.TopReadPercentage(identifier = "0.01", min = 43740),
            RemoteConfig.TopReadPercentage(identifier = "20", min = 4567, max = 8900),
            RemoteConfig.TopReadPercentage(identifier = "50", min = 336, max = 1233)
        )
    )

    @Test
    fun `logged-in user with 2 personalized pages gets personalized slides in order`() {
        val expected = listOf(
            YearInReviewPage.Cover(daysSpent = 2),
            YearInReviewPage.ArticlesRead(isEmptyState = false, count = 3, percentile = "", averageCount = YearInReviewConfig.fallbackConfig.averageArticlesReadPerYear),
            YearInReviewPage.Visits(days = 2, peakMonth = MonthVisitedDays(month = 1, visitedDays = 2)),
            YearInReviewPage.TopTopic(isEmptyState = true, topic = ArticleTopics.all[0], totalArticles = 0, sampleArticles = emptyList()),
            YearInReviewPage.RevisitedArticles(isEmptyState = true, articles = emptyList()),
            YearInReviewPage.TotalEdits(isEmptyState = true, editCount = 0),
            YearInReviewPage.YouMatter(showLoginPrompt = false),
            YearInReviewPage.End(isDonor = false)
        )
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = twoPersonalizedPagesStats)))
    }

    @Test
    fun `visits slide has no peak month when no month had 2 visited days`() {
        val stats = YearInReviewReadingStats(articlesReadCount = 3, visitedDaysCount = 2, peakMonth = 3, peakMonthVisitedDays = 1)
        val slides = YearInReviewFlowDecider.pages(snapshot(readingStats = stats))
        assertEquals(YearInReviewPage.Visits(days = 2, peakMonth = null), slides.filterIsInstance<YearInReviewPage.Visits>().single())
    }

    @Test
    fun `logged-in user with 1 personalized page gets collective slides`() {
        val expected = listOf(YearInReviewPage.Cover(daysSpent = 1)) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = false) + YearInReviewPage.End(isDonor = false)
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = onePersonalizedPageStats)))
    }

    @Test
    fun `logged-out user with 2 personalized pages gets collective slides with a login prompt`() {
        val expected = listOf(YearInReviewPage.Cover(daysSpent = 2)) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = true) + YearInReviewPage.End(isDonor = false)
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(isLoggedIn = false, readingStats = twoPersonalizedPagesStats)))
    }

    @Test
    fun `contributor with personalized slides gets thank you slide`() {
        val slides = YearInReviewFlowDecider.pages(snapshot(readingStats = twoPersonalizedPagesStats, rewardData = contributorRewardData))
        assertEquals(YearInReviewPage.ThankYou(isEditor = true, isDonor = true), slides[slides.size - 2])
    }

    @Test
    fun `contributor with collective slides gets you matter slide`() {
        val expected = listOf(YearInReviewPage.Cover(daysSpent = 1)) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = false) + YearInReviewPage.End(isDonor = true)
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = onePersonalizedPageStats, rewardData = contributorRewardData)))
    }

    @Test
    fun `articles read slide gets the percentile bucket of the article count`() {
        assertEquals("20", articlesReadPercentile(articlesReadCount = 5000))
    }

    @Test
    fun `articles read slide gets the top bucket when it has no max`() {
        assertEquals("0.01", articlesReadPercentile(articlesReadCount = 50000))
    }

    @Test
    fun `articles read slide gets no percentile below the lowest bucket`() {
        assertEquals("", articlesReadPercentile(articlesReadCount = 100))
    }

    private fun articlesReadPercentile(articlesReadCount: Int): String {
        val stats = twoPersonalizedPagesStats.copy(articlesReadCount = articlesReadCount)
        val slides = YearInReviewFlowDecider.pages(snapshot(readingStats = stats, remoteConfig = topReadPercentagesConfig))
        return slides.filterIsInstance<YearInReviewPage.ArticlesRead>().single().percentile
    }

    private fun snapshot(
        isLoggedIn: Boolean = true,
        isDonationEligible: Boolean = true,
        readingStats: YearInReviewReadingStats = YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0, peakMonth = 0, peakMonthVisitedDays = 0),
        editingStats: YearInReviewEditingStats = YearInReviewEditingStats(),
        rewardData: YearInReviewRewardData = YearInReviewRewardData(isDonor = false, isEditor = false),
        remoteConfig: RemoteConfig.RemoteConfigYearInReview = YearInReviewConfig.fallbackConfig
    ) = YearInReviewSnapshot(
        year = YearInReviewConfig.YEAR,
        isLoggedIn = isLoggedIn,
        isDonationEligible = isDonationEligible,
        readingStats = readingStats,
        editingStats = editingStats,
        rewardData = rewardData,
        remoteConfig = remoteConfig
    )
}
