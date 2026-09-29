package org.wikipedia.yearinreview.data

import org.wikipedia.settings.RemoteConfig

data class YearInReviewSnapshot(
    val year: Int,
    val isLoggedIn: Boolean,
    val isDonationEligible: Boolean,
    val remoteConfig: RemoteConfig.RemoteConfigYearInReview? = null,
    val readingStats: YearInReviewReadingStats,
    val editingStats: YearInReviewEditingStats? = null,
    val rewardData: YearInReviewRewardData
)
