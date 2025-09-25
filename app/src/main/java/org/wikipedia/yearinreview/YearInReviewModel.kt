package org.wikipedia.yearinreview

data class YearInReviewModel(
    val enReadingTimeSpent: Long,
    val enPopularArticles: List<String>,
    val enEditsCount: Long,
    val enBytesAddedCount: Long,
    val availableLanguages: Int,
    val globalTotalArticles: Long,
    val globalEditsCount: Long,
    val globalReadingArticlesCount: Int,
    val globalEditsPerMinute: Int,
    val appArticlesViewedTimes: Long,
    val appArticlesSavedTimes: Long,
    val appsEditsCount: Long,
    val localReadingTimeSpent: Long,
    val localReadingArticlesCount: Int,
    val localReadingRank: String, // TODO: TBD: top 50% or pure numbers,
    val localSavedArticles: List<String>,
    val localTopVisitedArticles: List<String>,
    val localTopCategories: List<String>,
    val favoriteTimeToRead: String,
    val favoriteDayToRead: String,
    val favoriteMonthDidMostReading: String,
    val closestLocation: Pair<Double, Double>,
    val closestArticles: List<String>,
    val userEditsCount: Int,
    val userEditsViewedTimes: Long
)
