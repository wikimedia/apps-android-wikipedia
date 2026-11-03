package org.wikipedia.yearinreview.presentation

import org.wikipedia.history.db.MonthVisitedDays
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.DateUtil
import org.wikipedia.yearinreview.data.YearInReviewInsightStatus
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

object YearInReviewFlowDecider {
    private const val MIN_PERSONALIZED_PAGES = 2

    // The page should only be built when the slide isn't suppressed, since its data may not exist otherwise.
    private class CandidatePage(val status: YearInReviewInsightStatus, val page: () -> YearInReviewPage)

    fun hasEnoughPersonalizedPages(snapshot: YearInReviewSnapshot): Boolean {
        val candidates = personalizedCandidates(snapshot)
        return hasEnoughPersonalizedPages(candidates)
    }

    fun canShareHighlights(snapshot: YearInReviewSnapshot): Boolean {
        var totalHighlights = 0
        if (snapshot.readingStats.articlesReadCount > 2) {
            totalHighlights++
        }
        if (snapshot.readingStats.totalTimeSpentSec.minutes > 10.minutes) {
            totalHighlights++
        }
        if (snapshot.readingStats.rabbitHoleStart != null && snapshot.readingStats.rabbitHoleEnd != null) {
            totalHighlights++
        }
        if (snapshot.readingStats.articlesRevisited.size > 1) {
            totalHighlights++
        }
        if (!snapshot.readingStats.topCategory.isNullOrEmpty()) {
            totalHighlights++
        }
        return totalHighlights >= 2
    }

    fun pages(snapshot: YearInReviewSnapshot): List<YearInReviewPage> {
        val candidates = personalizedCandidates(snapshot)
        val isPersonalized = snapshot.isLoggedIn && hasEnoughPersonalizedPages(candidates)
        val flowPages = if (isPersonalized) {
            candidates.filter { it.status != YearInReviewInsightStatus.SUPPRESSED }.map { it.page() } +
                    listOfNotNull(personalizedClosingPage(snapshot))
        } else {
            YearInReviewCollectiveInsight.entries.map { YearInReviewPage.Collective(it, snapshot.remoteConfig) } + listOfNotNull(youMatterPage(snapshot))
        }
        return flowPages + YearInReviewPage.End(isDonor = snapshot.rewardData.isContributor)
    }

    private fun personalizedCandidates(snapshot: YearInReviewSnapshot): List<CandidatePage> {
        val readingStats = snapshot.readingStats
        val editingStats = snapshot.editingStats
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
            CandidatePage(status = readingStats.totalReadingStatus) {
                YearInReviewPage.TimeSpent(
                    duration = readingStats.totalTimeSpentSec
                )
            },
            CandidatePage(status = readingStats.streakStatus) {
                YearInReviewPage.ReadingStreak(
                    isEmptyState = readingStats.streakStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    longestStreak = readingStats.longestStreakDays,
                    streakStartDate = if (readingStats.streakStatus != YearInReviewInsightStatus.EMPTY_STATE) DateUtil.getMonthOnlyDateString(LocalDate.of(LocalDate.now().year, readingStats.longestStreakStartMonth, readingStats.longestStreakStartDay)) else "",
                    streakEndDate = if (readingStats.streakStatus != YearInReviewInsightStatus.EMPTY_STATE) DateUtil.getMonthOnlyDateString(LocalDate.of(LocalDate.now().year, readingStats.longestStreakEndMonth, readingStats.longestStreakEndDay)) else ""
                )
            },
            CandidatePage(status = readingStats.readingPatternStatus) {
                YearInReviewPage.ReadingPattern(
                    isEmptyState = readingStats.readingPatternStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    favoriteTime = readingStats.favoriteTimeOfDay,
                    favoriteTimePercentage = readingStats.favoriteTimeOfDayPercentage
                )
            },
            CandidatePage(status = readingStats.topTopicsStatus) {
                YearInReviewPage.TopTopic(
                    isEmptyState = readingStats.topTopicsStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    topic = readingStats.topTopic ?: ArticleTopics.all.first(),
                    totalArticles = readingStats.topTopicTotalArticles,
                    sampleArticles = readingStats.topTopicSampleArticles.orEmpty()
                )
            },
            CandidatePage(status = readingStats.otherTopicsStatus) {
                YearInReviewPage.OtherTopTopics(
                    topicArticleCounts = readingStats.topicArticleCountMap
                )
            },
            CandidatePage(status = readingStats.longestReadArticleStatus) {
                YearInReviewPage.LongestReadGuess(
                    article = readingStats.longestReadArticles.firstOrNull(),
                    fakeArticle = readingStats.longestReadArticles.getOrNull(1),
                    minutesSpent = readingStats.longestReadArticleMinutes
                )
            },
            CandidatePage(status = readingStats.rabbitHoleStatus) {
                YearInReviewPage.RabbitHoleGuess(
                    articleStart = readingStats.rabbitHoleStart,
                    articleEnd = readingStats.rabbitHoleEnd,
                    fakeArticleStart = readingStats.rabbitHoleFakeStart,
                    fakeArticleEnd = readingStats.rabbitHoleFakeEnd,
                    numLinks = readingStats.rabbitHoleArticleCount
                )
            },
            CandidatePage(status = readingStats.articlesRevisitedStatus) {
                YearInReviewPage.RevisitedArticles(
                    isEmptyState = readingStats.articlesRevisitedStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    articles = readingStats.articlesRevisited
                )
            },
            CandidatePage(status = readingStats.topCategoryStatus) {
                YearInReviewPage.Category(
                    categoryName = readingStats.topCategory.orEmpty()
                )
            },
            CandidatePage(status = readingStats.topCountryStatus) {
                YearInReviewPage.Geography(
                    isEmptyState = readingStats.topCountryStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    countryCode = readingStats.topCountryCode.orEmpty(),
                    countryName = readingStats.topCountryName.orEmpty(),
                    articleCount = readingStats.topCountryArticleCount
                )
            },
            CandidatePage(status = readingStats.savedArticlesStatus) {
                YearInReviewPage.SavedArticles(
                    isEmptyState = readingStats.savedArticlesStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    savedCount = readingStats.articlesSavedTotal,
                    articles = readingStats.articlesSaved
                )
            },
            CandidatePage(status = editingStats.totalEditCountStatus) {
                YearInReviewPage.TotalEdits(
                    isEmptyState = editingStats.totalEditCountStatus == YearInReviewInsightStatus.EMPTY_STATE,
                    editCount = editingStats.totalEditCount
                )
            },
            CandidatePage(status = editingStats.totalPageViewsCountStatus) {
                YearInReviewPage.EditedArticleViews(
                    viewCount = editingStats.totalPageViewsCount
                )
            },
            CandidatePage(status = editingStats.mostViewedArticlesStatus) {
                YearInReviewPage.MostViewedEditedArticles(
                    articles = editingStats.mostViewedArticles
                )
            }
        )
    }

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
        return snapshot.remoteConfig.topReadPercentages.find {
            snapshot.readingStats.articlesReadCount > it.min && snapshot.readingStats.articlesReadCount <= it.max
        }?.identifier.orEmpty()
    }
}
