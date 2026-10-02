package org.wikipedia.yearinreview.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wikipedia.yearinreview.presentation.YearInReviewCollectiveInsight
import org.wikipedia.yearinreview.presentation.YearInReviewFlowDecider
import org.wikipedia.yearinreview.presentation.YearInReviewPage

class YearInReviewFlowDeciderTest {

    private val dataRichStats = YearInReviewReadingStats(articlesReadCount = 3, visitedDaysCount = 2)
    private val lowDataStats = YearInReviewReadingStats(articlesReadCount = 3, visitedDaysCount = 1)
    private val contributorRewardData = YearInReviewRewardData(isDonor = true, isEditor = true)
    private val collectiveSlides = YearInReviewCollectiveInsight.entries.map { YearInReviewPage.Collective(it) }

    @Test
    fun `logged-in data-rich user gets personalized slides in order`() {
        val expected = listOf(
            YearInReviewPage.Cover,
            YearInReviewPage.ArticlesRead(isEmptyState = false, count = 3),
            YearInReviewPage.Visits(days = 2),
            YearInReviewPage.YouMatter(showLoginPrompt = false),
            YearInReviewPage.End
        )
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = dataRichStats)))
    }

    @Test
    fun `logged-in low-data user gets collective slides`() {
        val expected = listOf(YearInReviewPage.Cover) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = false) + YearInReviewPage.End
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = lowDataStats)))
    }

    @Test
    fun `logged-out data-rich user gets collective slides with a login prompt`() {
        val expected = listOf(YearInReviewPage.Cover) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = true) + YearInReviewPage.End
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(isLoggedIn = false, readingStats = dataRichStats)))
    }

    @Test
    fun `data-rich contributor gets thank you slide`() {
        val slides = YearInReviewFlowDecider.pages(snapshot(readingStats = dataRichStats, rewardData = contributorRewardData))
        assertEquals(YearInReviewPage.ThankYou(isEditor = true, isDonor = true), slides[slides.size - 2])
    }

    @Test
    fun `low-data contributor gets you matter slide`() {
        val expected = listOf(YearInReviewPage.Cover) + collectiveSlides + YearInReviewPage.YouMatter(showLoginPrompt = false) + YearInReviewPage.End
        assertEquals(expected, YearInReviewFlowDecider.pages(snapshot(readingStats = lowDataStats, rewardData = contributorRewardData)))
    }

    private fun snapshot(
        isLoggedIn: Boolean = true,
        isDonationEligible: Boolean = true,
        readingStats: YearInReviewReadingStats = YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0),
        rewardData: YearInReviewRewardData = YearInReviewRewardData(isDonor = false, isEditor = false)
    ) = YearInReviewSnapshot(
        year = YearInReviewConfig.YEAR,
        isLoggedIn = isLoggedIn,
        isDonationEligible = isDonationEligible,
        readingStats = readingStats,
        rewardData = rewardData
    )
}
