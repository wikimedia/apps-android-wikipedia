package org.wikipedia.yearinreview

sealed class YearInReviewScreenData(
    val enReadingTimeInHours: Long,
    val enPopularArticles: List<String>,
    val availableLanguages: Int,
    val globalTotalArticles: Long,
    val articlesViewedTimes: Long,
    val articlesSavedTimes: Long,
    val localReadingTimeInMinutes: Long,
    val localReadingArticles: Int,
    val localReadingRank: String, // TODO: TBD: top 50% or pure numbers,
    val localSavedArticles: List<String>,
    val topArticles: List<String>,
    val favoriteTimeToRead: String,
    val favoriteDayToRead: String,
    val favoriteMonthDidMostReading: String,
    val topCategories: List<String>,
    val closestLocation: Pair<Double, Double>,
    val closestArticles: List<String>,
    val userEditsCount: Long,
    val globalEditsCount: Long,
    val enEditsCount: Long,
    val userEditsViewedTimes: Long,
    val appsEditsCount: Long,
    val editsPerMinute: Long,
    val enBytesAddedCount: Long
)
