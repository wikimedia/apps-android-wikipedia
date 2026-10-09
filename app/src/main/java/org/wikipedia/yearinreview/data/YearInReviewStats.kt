package org.wikipedia.yearinreview.data

import kotlinx.serialization.Serializable
import org.wikipedia.topics.ArticleTopic

@Serializable
data class YearInReviewCachedStats(
    val readingStats: YearInReviewReadingStats?,
    val editingStats: YearInReviewEditingStats?
)

@Serializable
data class YearInReviewEditingStats(
    val contributionsEditCount: Int
)

@Serializable
data class YearInReviewReadingStats(
    val articlesReadCount: Int,
    val visitedDaysCount: Int,
    val peakMonth: Int,
    val peakMonthVisitedDays: Int,
    val topTopic: ArticleTopic? = null,
    val topTopicArticles: List<String>? = emptyList()
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

    val topTopicsStatus get() = if (topTopic != null && (topTopicArticles?.size ?: 0) >= MIN_ARTICLES_READ) {
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

data class YearInReviewRewardData(
    val isDonor: Boolean,
    val isEditor: Boolean
) {
    val isContributor = isDonor || isEditor
}
