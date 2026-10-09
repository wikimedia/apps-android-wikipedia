package org.wikipedia.yearinreview.presentation

import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.topics.ArticleTopics
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
        return listOf(YearInReviewPage.Cover(daysSpent = snapshot.readingStats.visitedDaysCount)) + flowPages + YearInReviewPage.End
    }

    private fun personalizedCandidates(snapshot: YearInReviewSnapshot): List<CandidatePage> {
        val readingStats = snapshot.readingStats
        return listOf(
            CandidatePage(status = readingStats.totalArticlesStatus) {
                YearInReviewPage.ArticlesRead(
                    isEmptyState = readingStats.totalArticlesStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    count = readingStats.articlesReadCount,
                    percentile = getTopReadPercentile(snapshot),
                    averageCount = snapshot.remoteConfig.averageArticlesReadPerYear
                )
            },
            CandidatePage(status = readingStats.visitsStatus) {
                YearInReviewPage.Visits(
                    days = readingStats.visitedDaysCount,
                    peakMonth = if (readingStats.hasPeakMonth) MonthVisitedDays(readingStats.peakMonth, readingStats.peakMonthVisitedDays) else null
                )
            },
            CandidatePage(status = readingStats.topTopicsStatus) {
                YearInReviewPage.TopTopic(
                    isEmptyState = readingStats.topTopicsStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    topic = readingStats.topTopic ?: ArticleTopics.all.first(),
                    articles = readingStats.topTopicArticles.orEmpty()
                )
            }
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

    private fun getTopReadPercentile(snapshot: YearInReviewSnapshot): String {
        return snapshot.remoteConfig?.topReadPercentages?.find {
            snapshot.readingStats.articlesReadCount > it.min && snapshot.readingStats.articlesReadCount <= it.max
        }?.identifier.orEmpty()
    }
}
