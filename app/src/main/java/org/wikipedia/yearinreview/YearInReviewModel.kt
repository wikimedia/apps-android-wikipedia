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
    val closestLocation: Pair<Double, Double>,
    val closestArticles: List<String>,
    val userEditsCount: Int,
    val userEditsViewedTimes: Long,
    val isCustomIconUnlocked: Boolean
)
