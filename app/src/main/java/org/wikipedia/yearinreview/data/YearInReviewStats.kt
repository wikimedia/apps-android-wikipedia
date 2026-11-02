package org.wikipedia.yearinreview.data

import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopic

@Serializable
data class YearInReviewCachedStats(
    val readingStats: YearInReviewReadingStats?,
    val editingStats: YearInReviewEditingStats?
)

@Serializable
data class YearInReviewEditingStats(
    val totalEditCount: Int = 0,
    val totalPageViewsCount: Long = 0,
    val mostViewedArticles: List<Pair<PageTitle, Long>> = emptyList()
) {
    val totalEditCountStatus get() = if (totalEditCount > 0) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val totalPageViewsCountStatus get() = if (totalPageViewsCount > 25) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val mostViewedArticlesStatus get() = if (totalPageViewsCount > 25 && mostViewedArticles.size >= 2) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }
}

@Serializable
data class YearInReviewReadingStats(
    val articlesReadCount: Int,
    val visitedDaysCount: Int,
    val totalTimeSpentSec: Long = 0,
    val peakMonth: Int,
    val peakMonthVisitedDays: Int,
    val articlesRevisited: List<Pair<PageTitle, Int>> = emptyList(),
    val articlesSavedTotal: Int = 0,
    val articlesSaved: List<PageTitle> = emptyList(),
    val topCategory: String? = null,
    val topCountryCode: String? = null,
    val topCountryName: String? = null,
    val topCountryArticleCount: Int = 0,
    val favoriteTimeOfDay: YearInReviewTimeOfDay? = null,
    val favoriteTimeOfDayPercentage: Int = 0,
    val longestStreakDays: Int = 0,
    val longestStreakStartMonth: Int = 0, // 1 = January, as in java.time.Month
    val longestStreakStartDay: Int = 0,
    val longestStreakEndMonth: Int = 0,
    val longestStreakEndDay: Int = 0,
    val biggestReadingDayMonth: Int = 0, // 1 = January, as in java.time.Month
    val biggestReadingDayOfMonth: Int = 0,
    val biggestReadingDayTimeSpentSec: Int = 0,
    val biggestReadingDayArticles: List<PageTitle> = emptyList(),
    val longestReadArticles: List<PageTitle> = emptyList(),
    val longestReadArticleMinutes: Int = 0,
    val rabbitHoleStart: PageTitle? = null,
    val rabbitHoleEnd: PageTitle? = null,
    val rabbitHoleFakeStart: PageTitle? = null,
    val rabbitHoleFakeEnd: PageTitle? = null,
    val rabbitHoleArticleCount: Int = 0,
    val topTopic: ArticleTopic? = null,
    val topTopicTotalArticles: Int = 0,
    val topTopicSampleArticles: List<String>? = emptyList(),
    val topicArticleCountMap: Map<String, Int> = emptyMap()
) {
    val totalArticlesStatus get() = if (articlesReadCount >= MIN_ARTICLES_READ) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val visitsStatus get() = if (visitedDaysCount >= MIN_VISITED_DAYS) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val totalReadingStatus get() = if (totalTimeSpentSec in 61..<18000000) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val streakStatus get() = if (longestStreakDays > 1 && longestStreakStartMonth > 0 && longestStreakStartDay > 0 && longestStreakEndMonth > 0 && longestStreakEndDay > 0) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val readingPatternStatus get() = if (favoriteTimeOfDay != null && favoriteTimeOfDayPercentage > 0) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val topTopicsStatus get() = if (topTopic != null && topTopicTotalArticles >= 3 && articlesReadCount >= 5) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val otherTopicsStatus get() = if (topTopic != null && topicArticleCountMap.size >= 2) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val articlesRevisitedStatus get() = if (articlesRevisited.size >= 2) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val longestReadArticleStatus get() = if (longestReadArticles.size > 1 && longestReadArticleMinutes > 10) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val rabbitHoleStatus get() = if (rabbitHoleStart != null && rabbitHoleEnd != null && rabbitHoleArticleCount >= 3 && rabbitHoleFakeStart != null && rabbitHoleFakeEnd != null) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val topCategoryStatus get() = if (!topCategory.isNullOrEmpty()) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.SUPPRESSED
    }

    val topCountryStatus get() = if (!topCountryCode.isNullOrEmpty()) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val savedArticlesStatus get() = if (articlesSavedTotal > 0 && articlesSaved.isNotEmpty()) {
        YearInReviewInsightStatus.PERSONALIZED
    } else {
        YearInReviewInsightStatus.EMPTY_STATE
    }

    val hasPeakMonth get() = peakMonthVisitedDays >= MIN_PEAK_MONTH_VISITED_DAYS

    companion object {
        const val MIN_ARTICLES_READ = 3
        const val MIN_VISITED_DAYS = 2
        const val MIN_PEAK_MONTH_VISITED_DAYS = 2
    }
}

data class YearInReviewEntryStats(
    val articlesReadCount: Int,
    val visitedDaysCount: Int
) {
    val isDataRich get() = articlesReadCount >= YearInReviewReadingStats.MIN_ARTICLES_READ
}

data class YearInReviewRewardData(
    val isDonor: Boolean,
    val isEditor: Boolean
) {
    val isContributor = isDonor || isEditor
}
