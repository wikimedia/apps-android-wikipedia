package org.wikipedia.yearinreview.data

enum class YearInReviewInsightStatus {
    PERSONALIZED,
    EMPTY_STATE,
    SUPPRESSED
}

enum class YearInReviewAudience {
    LOW_DATA,
    DATA_RICH
}

object YearInReviewAudienceClassifier {
    private const val MIN_PERSONALIZED_INSIGHTS = 2

    fun classify(statuses: Collection<YearInReviewInsightStatus>): YearInReviewAudience {
        return if (statuses.count { it == YearInReviewInsightStatus.PERSONALIZED } >= MIN_PERSONALIZED_INSIGHTS) {
            YearInReviewAudience.DATA_RICH
        } else {
            YearInReviewAudience.LOW_DATA
        }
    }
}
