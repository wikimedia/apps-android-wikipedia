package org.wikipedia.yearinreview.data

import org.wikipedia.settings.RemoteConfig

data class YearInReviewSnapshot(
    val year: Int,
    val isDonationEligible: Boolean,
    val remoteConfig: RemoteConfig.RemoteConfigYearInReview? = null,
    val readingStats: YearInReviewReadingStats? = null,
    val editingStats: YearInReviewEditingStats? = null,
    val rewardData: YearInReviewRewardData
)
