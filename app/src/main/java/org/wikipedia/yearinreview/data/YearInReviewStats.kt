package org.wikipedia.yearinreview.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.history.db.HistoryEntryWithImage

@Serializable
data class YearInReviewCachedStats(
    val readingStats: YearInReviewReadingStats?,
    val editingStats: YearInReviewEditingStats?
)

@Serializable
data class YearInReviewEditingStats(
    val userEditsCount: Int,
    val userEditsViewedTimes: Long
)

@Serializable
data class YearInReviewReadingStats(
    val totalReadingTimeMinutes: Long,
    val localReadingArticlesCount: Int,
    val localSavedArticlesCount: Int,
    val localSavedArticles: List<String>,
    val localTopVisitedArticles: List<String>,
    val localTopCategories: List<String>,
    val favoriteTimeToRead: Int,
    val favoriteDayToRead: Int,
    val favoriteMonthDidMostReading: Int,
    val geoStats: YearInReviewGeoStats
)

@Serializable
data class YearInReviewGeoStats(
    val largestClusterLocation: Pair<Double, Double>,
    val largestClusterTopLeft: Pair<Double, Double>,
    val largestClusterBottomRight: Pair<Double, Double>,
    val largestClusterCountryName: String,
    val largestClusterArticles: List<String>,
    @Transient val pagesWithCoordinates: List<HistoryEntryWithImage> = emptyList()
)

data class YearInReviewRewardData(
    val isDonor: Boolean,
    val isEditor: Boolean
) {
    val isCustomIconUnlocked = isDonor || isEditor
}
