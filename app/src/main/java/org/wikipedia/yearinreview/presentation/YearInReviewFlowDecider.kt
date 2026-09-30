package org.wikipedia.yearinreview.presentation

import org.wikipedia.yearinreview.data.YearInReviewInsightStatus
import org.wikipedia.yearinreview.data.YearInReviewSnapshot

object YearInReviewFlowDecider {
    private const val MIN_PERSONALIZED_PAGES = 2

    // The page should only be built when the slide isn't suppressed, since its data may not exist otherwise.
    private class CandidatePage(val status: YearInReviewInsightStatus, val page: () -> YearInReviewPage)

    fun isDataRich(snapshot: YearInReviewSnapshot): Boolean {
        val candidates = personalizedCandidates(snapshot)
        return hasEnoughPersonalizedPages(candidates)
    }

    fun pages(snapshot: YearInReviewSnapshot): List<YearInReviewPage> {
        val candidates = personalizedCandidates(snapshot)
        val hasEnoughData = hasEnoughPersonalizedPages(candidates)
        val flowPages = if (snapshot.isLoggedIn && hasEnoughData) {
            candidates.filter { it.status != YearInReviewInsightStatus.SUPPRESSED }.map { it.page() } +
                    listOfNotNull(personalizedClosingPage(snapshot))
        } else {
            YearInReviewCollectiveInsight.entries.map { YearInReviewPage.Collective(it) } + listOfNotNull(youMatterPage(snapshot))
        }
        return flowPages + YearInReviewPage.End
    }

    private fun personalizedCandidates(snapshot: YearInReviewSnapshot): List<CandidatePage> {
        val readingStats = snapshot.readingStats
        return listOf(
            CandidatePage(status = readingStats.totalArticlesStatus) {
                YearInReviewPage.ArticlesRead(isEmptyState = readingStats.totalArticlesStatus == YearInReviewInsightStatus.EMPTY_STATE)
            },
            CandidatePage(status = readingStats.visitsStatus) { YearInReviewPage.Visits }
        )
    }

    // TODO: confirm this logic
    private fun personalizedClosingPage(snapshot: YearInReviewSnapshot): YearInReviewPage? {
        val rewardData = snapshot.rewardData
        return if (rewardData.isContributor) {
            YearInReviewPage.ThankYou(isEditor = rewardData.isEditor, isDonor = rewardData.isDonor)
        } else {
            youMatterPage(snapshot)
        }
    }

    private fun hasEnoughPersonalizedPages(candidates: List<CandidatePage>): Boolean {
        return candidates.count { it.status == YearInReviewInsightStatus.PERSONALIZED } >= MIN_PERSONALIZED_PAGES
    }

    private fun youMatterPage(snapshot: YearInReviewSnapshot): YearInReviewPage? {
        return if (snapshot.isDonationEligible) YearInReviewPage.YouMatter(showLoginPrompt = !snapshot.isLoggedIn) else null
    }
}
