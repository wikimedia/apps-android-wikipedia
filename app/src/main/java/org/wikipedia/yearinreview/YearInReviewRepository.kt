package org.wikipedia.yearinreview

import android.location.Geocoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.CategoryDao
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.GeoUtil.LocationClusterer
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface YearInReviewRepository {
    suspend fun getYearInReview(year: Int): YearInReviewSnapshot
}

class YearInReviewRepositoryImpl(
    private val restService: RestService = ServiceFactory.getRest(WikipediaApp.instance.wikiSite),
    private val historyEntryDao: HistoryEntryDao = AppDatabase.instance.historyEntryDao(),
    private val historyEntryWithImageDao: HistoryEntryWithImageDao = AppDatabase.instance.historyEntryWithImageDao(),
    private val readingListPageDao: ReadingListPageDao = AppDatabase.instance.readingListPageDao(),
    private val categoryDao: CategoryDao = AppDatabase.instance.categoryDao()
) : YearInReviewRepository {

    override suspend fun getYearInReview(year: Int): YearInReviewSnapshot = coroutineScope {
        val remoteConfig = restService.getConfiguration().commonv1?.getYirForYear(year)
        val isDonationEligible = remoteConfig != null && !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty())
        val dataStartMillis = remoteConfig?.dataStartDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
        val dataEndMillis = remoteConfig?.dataEndDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

        val cachedStats = Prefs.yearInReviewCachedStats[year]
        val editingStats: YearInReviewEditingStats?
        val readingStats: YearInReviewReadingStats?
        if (cachedStats != null) {
            editingStats = cachedStats.editingStats
            readingStats = if (cachedStats.readingStats != null && dataStartMillis != null && dataEndMillis != null) {
                val pagesWithCoordinates = getPagesWithCoordinates(dataStartMillis, dataEndMillis)
                cachedStats.readingStats.copy(geoStats = cachedStats.readingStats.geoStats.copy(pagesWithCoordinates = pagesWithCoordinates))
            } else {
                cachedStats.readingStats
            }
        } else {
            val editingStatsDeferred = async { getEditingStats(year) }
            val readingStatsDeferred = async {
                if (dataStartMillis != null && dataEndMillis != null) getReadingStats(year, dataStartMillis, dataEndMillis) else null
            }
            editingStats = editingStatsDeferred.await()
            readingStats = readingStatsDeferred.await()
            if (readingStats != null) {
                Prefs.yearInReviewCachedStats += (year to YearInReviewCachedStats(readingStats, editingStats))
                YearInReviewDialog.resetYearInReviewSurveyState()
            }
        }

        YearInReviewSnapshot(
            year = year,
            isDonationEligible = isDonationEligible,
            remoteConfig = remoteConfig,
            readingStats = readingStats,
            editingStats = editingStats,
            rewardData = YearInReviewRewardData(
                isDonor = Prefs.donationResults.isNotEmpty(),
                isEditor = editingStats?.userEditsCount?.let { it > 0 } == true
            )
        )
    }

    private suspend fun getPagesWithCoordinates(startMillis: Long, endMillis: Long): List<HistoryEntryWithImage> {
        return historyEntryWithImageDao.getEntriesWithCoordinates(256, startMillis, endMillis)
            .distinctBy { it.apiTitle }
    }

    private suspend fun getReadingStats(year: Int, startMillis: Long, endMillis: Long): YearInReviewReadingStats = coroutineScope {
        val totalReadingTimeMinutes = async { historyEntryWithImageDao.getTimeSpentBetween(startMillis, endMillis) / 60 }
        val localReadingArticlesCount = async { historyEntryDao.getDistinctEntriesCountBetween(startMillis, endMillis) }
        val localSavedArticlesCount = async { readingListPageDao.getTotalSavedPagesBetween(startMillis, endMillis) ?: 0 }
        val localSavedArticles = async {
            readingListPageDao.getRandomPageTitlesBetween(MIN_SAVED_ARTICLES, startMillis, endMillis)
                .map { StringUtil.fromHtml(it).toString() }
                .filter { it.isNotBlank() }
        }
        val localTopVisitedArticles = async {
            historyEntryDao.getTopVisitedEntriesBetween(MAX_TOP_ARTICLES, startMillis, endMillis)
                .map { StringUtil.fromHtml(it).toString() }
                .filter { it.isNotBlank() }
        }
        val localTopCategories = async { getTopVisitedCategories(year) }
        val favoriteTimeToRead = async { historyEntryDao.getFavoriteTimeToReadBetween(startMillis, endMillis) ?: 0 }
        val favoriteDayToRead = async {
            historyEntryDao.getFavoriteDayToReadBetween(startMillis, endMillis)?.let { if (it == 0) 7 else it } ?: 1
        }
        val favoriteMonthDidMostReading = async { historyEntryDao.getMostReadingMonthBetween(startMillis, endMillis) ?: 1 }
        val geoStats = async { getGeoStats(startMillis, endMillis) }

        YearInReviewReadingStats(
            totalReadingTimeMinutes = totalReadingTimeMinutes.await(),
            localReadingArticlesCount = localReadingArticlesCount.await(),
            localSavedArticlesCount = localSavedArticlesCount.await(),
            localSavedArticles = localSavedArticles.await(),
            localTopVisitedArticles = localTopVisitedArticles.await(),
            localTopCategories = localTopCategories.await(),
            favoriteTimeToRead = favoriteTimeToRead.await(),
            favoriteDayToRead = favoriteDayToRead.await(),
            favoriteMonthDidMostReading = favoriteMonthDidMostReading.await(),
            geoStats = geoStats.await()
        )
    }

    private suspend fun getGeoStats(startMillis: Long, endMillis: Long): YearInReviewGeoStats {
        var pagesWithCoordinates = getPagesWithCoordinates(startMillis, endMillis)

        var largestClusterLatitude = 0.0
        var largestClusterLongitude = 0.0
        var largestClusterTopLeft = Pair(0.0, 0.0)
        var largestClusterBottomRight = Pair(0.0, 0.0)
        var largestClusterCountryName = ""
        val largestClusterArticles = mutableListOf<String>()
        if (pagesWithCoordinates.size > MIN_ARTICLES_PER_MAP_CLUSTER) {
            try {
                val clusters = LocationClusterer().clusterLocations(
                    locations = pagesWithCoordinates,
                    epsilonKm = 500.0,
                    minPoints = 3
                )
                val largestCluster = clusters.maxByOrNull { it.locations.size }
                if (largestCluster != null && largestCluster.centroid != null && largestCluster.locations.size >= MIN_ARTICLES_PER_MAP_CLUSTER) {
                    largestClusterArticles.addAll(largestCluster.locations.map { it.displayTitle }.take(MIN_ARTICLES_PER_MAP_CLUSTER))
                    largestClusterLatitude = largestCluster.centroid.latitude
                    largestClusterLongitude = largestCluster.centroid.longitude

                    val largestClusterBounds = LatLngBounds.Builder()
                    largestCluster.locations.forEach {
                        largestClusterBounds.include(LatLng(it.geoLat ?: 0.0, it.geoLon ?: 0.0))
                    }
                    val bounds = largestClusterBounds.build()
                    largestClusterTopLeft = Pair(bounds.latitudeNorth, bounds.longitudeEast)
                    largestClusterBottomRight = Pair(bounds.latitudeSouth, bounds.longitudeWest)

                    val geocoder = Geocoder(WikipediaApp.instance)
                    val results = geocoder.getFromLocation(largestClusterLatitude, largestClusterLongitude, 2)
                    if (!results.isNullOrEmpty()) {
                        largestClusterCountryName = results.first().countryName.orEmpty()
                    }
                    pagesWithCoordinates = largestCluster.locations.plus(pagesWithCoordinates.minus(
                        largestCluster.locations.toSet()
                    ))
                }
            } catch (_: IOException) {
                // could be thrown by Geocoder, and safe to ignore.
            }
        }

        return YearInReviewGeoStats(
            largestClusterLocation = Pair(largestClusterLatitude, largestClusterLongitude),
            largestClusterTopLeft = largestClusterTopLeft,
            largestClusterBottomRight = largestClusterBottomRight,
            largestClusterCountryName = largestClusterCountryName,
            largestClusterArticles = largestClusterArticles,
            pagesWithCoordinates = pagesWithCoordinates
        )
    }

    private suspend fun getEditingStats(year: Int): YearInReviewEditingStats? {
        if (!AccountUtil.isLoggedIn) {
            return null
        }
        val wikiSite = WikipediaApp.instance.wikiSite
        val userInfoResponse = ServiceFactory.get(wikiSite).getLocalAndGlobalUserInfo()
        return coroutineScope {
            val editCount = async { getEditCount(year, userInfoResponse.query?.globalUserInfo?.id ?: 0) }
            val editedPageViews = async { getEditedPageViews(wikiSite, userInfoResponse.query?.userInfo?.id ?: 0) }
            YearInReviewEditingStats(
                userEditsCount = editCount.await(),
                userEditsViewedTimes = editedPageViews.await()
            )
        }
    }

    private suspend fun getEditCount(year: Int, globalUserId: Int): Int {
        return try {
            ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getEditsPerGlobalUserMonthly(
                    globalUserId,
                    DateUtil.getYMDDateString(LocalDate.of(year, 1, 1)),
                    DateUtil.getYMDDateString(LocalDate.of(year, 12, 31))
                ).items.sumOf { it.editCount }
        } catch (e: IOException) {
            L.e(e)
            0
        }
    }

    private suspend fun getEditedPageViews(wikiSite: WikiSite, userId: Int): Long {
        return try {
            val now = Instant.now().epochSecond
            val impactLastResponseBodyMap = Prefs.impactLastResponseBody.toMutableMap()
            val impactResponse = impactLastResponseBodyMap[wikiSite.languageCode]
            val impact: GrowthUserImpact
            if (impactResponse.isNullOrEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.HOURS.toSeconds(12)) {
                impact = ServiceFactory.getCoreRest(wikiSite).getUserImpact(userId)
                impactLastResponseBodyMap[wikiSite.languageCode] = JsonUtil.encodeToString(impact).orEmpty()
                Prefs.impactLastResponseBody = impactLastResponseBodyMap
                Prefs.impactLastQueryTime = now
            } else {
                impact = JsonUtil.decodeFromString(impactResponse)!!
            }
            impact.totalPageviewsCount
        } catch (e: IOException) {
            L.e(e)
            0L
        }
    }

    private suspend fun getTopVisitedCategories(year: Int): List<String> {
        val categories = categoryDao.getTopCategoriesByYear(year = year, limit = MAX_TOP_CATEGORY * 10)
            .map { StringUtil.removeNamespace(it.title) }
            .filter { it.isNotBlank() }
        val (categoriesWithTwoSpaces, remainingCategories) = categories.partition { category -> category.count { it == ' ' } >= 2 }
        return (categoriesWithTwoSpaces + remainingCategories).take(MAX_TOP_CATEGORY)
    }

    companion object {
        const val MIN_SAVED_ARTICLES = 3
        const val MAX_TOP_ARTICLES = 5
        const val MAX_TOP_CATEGORY = 5
        const val MIN_ARTICLES_PER_MAP_CLUSTER = 2
    }
}

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
