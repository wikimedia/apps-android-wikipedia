package org.wikipedia.yearinreview

import kotlinx.serialization.Serializable

@Serializable
data class YearInReviewModel(
    val totalReadingTimeMinutes: Long,
    val localReadingArticlesCount: Int,
    val localSavedArticlesCount: Int,
    val localSavedArticles: List<String>,
    val localTopVisitedArticles: List<String>,
    val localTopCategories: List<String>,
    val favoriteTimeToRead: Int,
    val favoriteDayToRead: Int,
    val favoriteMonthDidMostReading: Int,
    val largestClusterLocation: Pair<Double, Double>,
    val largestClusterTopLeft: Pair<Double, Double>,
    val largestClusterBottomRight: Pair<Double, Double>,
    val largestClusterCountryName: String,
    val largestClusterArticles: List<String>,
    val userEditsCount: Int,
    val userEditsViewedTimes: Long,
    val isCustomIconUnlocked: Boolean,
    val slideViewedCount: Int = 0,
    val isReadingListDialogShown: Boolean = false
)
