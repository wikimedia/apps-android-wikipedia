package org.wikipedia.yearinreview.data

import kotlinx.serialization.Serializable

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
    val peakMonthVisitedDays: Int
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

    companion object {
        const val MIN_ARTICLES_READ = 3
        const val MIN_VISITED_DAYS = 2
    }
}

data class YearInReviewRewardData(
    val isDonor: Boolean,
    val isEditor: Boolean
) {
    val isContributor = isDonor || isEditor
}
