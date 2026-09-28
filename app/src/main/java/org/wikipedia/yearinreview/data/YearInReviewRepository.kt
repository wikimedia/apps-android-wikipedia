package org.wikipedia.yearinreview.data

import android.location.Geocoder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface YearInReviewRepository {
    suspend fun getYearInReview(): YearInReviewSnapshot
}

class YearInReviewRepositoryImpl(
    private val restService: RestService = ServiceFactory.getRest(WikipediaApp.instance.wikiSite),
    private val historyEntryDao: HistoryEntryDao = AppDatabase.instance.historyEntryDao(),
    private val historyEntryWithImageDao: HistoryEntryWithImageDao = AppDatabase.instance.historyEntryWithImageDao(),
    private val readingListPageDao: ReadingListPageDao = AppDatabase.instance.readingListPageDao(),
    private val categoryDao: CategoryDao = AppDatabase.instance.categoryDao(),
    private val cache: YearInReviewCache = PrefsYearInReviewStore,
    private val donationEligibility: YearInReviewDonationEligibility = YearInReviewDonationEligibility()
) : YearInReviewRepository {

    private val maxTopCategory = 5
    private val minSavedArticles = 3
    private val maxTopArticles = 5
    private val minArticlesPerMapCluster = 2

    override suspend fun getYearInReview(): YearInReviewSnapshot = coroutineScope {
        val year = YearInReviewConfig.YEAR
        val remoteConfig = restService.getConfiguration().commonv1?.getYirForYear(year)
        val insightsDateRange = YearInReviewConfig.insightsDateRange(remoteConfig)
        val isDonationEligible = remoteConfig != null && !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty())

        val cachedStats = cache.get(year)
        val needsEditingStats = AccountUtil.isLoggedIn && cachedStats?.editingStats == null
        val editingStatsDeferred = if (needsEditingStats) {
            async { getEditingStats(YearInReviewConfig.contributionsDateRange(remoteConfig)) }
        } else {
            null
        }
        val readingStatsDeferred = if (cachedStats?.readingStats == null) {
            async { getReadingStats(insightsDateRange) }
        } else {
            null
        }
        val editingStats = cachedStats?.editingStats ?: editingStatsDeferred?.await()
        val readingStats = cachedStats?.readingStats?.let {
            val pagesWithCoordinates = getPagesWithCoordinates(insightsDateRange)
            it.copy(geoStats = it.geoStats.copy(pagesWithCoordinates = pagesWithCoordinates))
        } ?: readingStatsDeferred!!.await()
        if (cachedStats?.readingStats == null || needsEditingStats) {
            cache.put(year, YearInReviewCachedStats(readingStats, editingStats))
            Prefs.yearInReviewSurveyState = YearInReviewSurveyState.NOT_TRIGGERED
        }

        YearInReviewSnapshot(
            year = year,
            isDonationEligible = isDonationEligible,
            remoteConfig = remoteConfig,
            readingStats = readingStats,
            editingStats = editingStats,
            rewardData = YearInReviewRewardData(
                isDonor = donationEligibility.hasDonatedWithinContributionsDateRange(remoteConfig),
                isEditor = editingStats?.userEditsCount?.let { it > 0 } == true
            )
        )
    }

    private suspend fun getPagesWithCoordinates(dateRange: YearInReviewDateRange): List<HistoryEntryWithImage> {
        return historyEntryWithImageDao.getEntriesWithCoordinates(256, dateRange.startMillis, dateRange.endMillis)
            .distinctBy { it.apiTitle }
    }

    private suspend fun getReadingStats(dateRange: YearInReviewDateRange): YearInReviewReadingStats = coroutineScope {
        val startMillis = dateRange.startMillis
        val endMillis = dateRange.endMillis
        val totalReadingTimeMinutes = async { historyEntryWithImageDao.getTimeSpentBetween(startMillis, endMillis) / 60 }
        val localReadingArticlesCount = async { historyEntryDao.getDistinctEntriesCountBetween(startMillis, endMillis) }
        val localSavedArticlesCount = async { readingListPageDao.getTotalSavedPagesBetween(startMillis, endMillis) ?: 0 }
        val localSavedArticles = async {
            readingListPageDao.getRandomPageTitlesBetween(minSavedArticles, startMillis, endMillis)
                .map { StringUtil.fromHtml(it).toString() }
                .filter { it.isNotBlank() }
        }
        val localTopVisitedArticles = async {
            historyEntryDao.getTopVisitedEntriesBetween(maxTopArticles, startMillis, endMillis)
                .map { StringUtil.fromHtml(it).toString() }
                .filter { it.isNotBlank() }
        }
        val localTopCategories = async { getTopVisitedCategories(dateRange) }
        val favoriteTimeToRead = async { historyEntryDao.getFavoriteTimeToReadBetween(startMillis, endMillis) ?: 0 }
        val favoriteDayToRead = async {
            historyEntryDao.getFavoriteDayToReadBetween(startMillis, endMillis)?.let { if (it == 0) 7 else it } ?: 1
        }
        val favoriteMonthDidMostReading = async { historyEntryDao.getMostReadingMonthBetween(startMillis, endMillis) ?: 1 }
        val geoStats = async { getGeoStats(dateRange) }

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

    private suspend fun getGeoStats(dateRange: YearInReviewDateRange): YearInReviewGeoStats {
        var pagesWithCoordinates = getPagesWithCoordinates(dateRange)

        var largestClusterLatitude = 0.0
        var largestClusterLongitude = 0.0
        var largestClusterTopLeft = Pair(0.0, 0.0)
        var largestClusterBottomRight = Pair(0.0, 0.0)
        var largestClusterCountryName = ""
        val largestClusterArticles = mutableListOf<String>()
        if (pagesWithCoordinates.size > minArticlesPerMapCluster) {
            try {
                val clusters = LocationClusterer().clusterLocations(
                    locations = pagesWithCoordinates,
                    epsilonKm = 500.0,
                    minPoints = 3
                )
                val largestCluster = clusters.maxByOrNull { it.locations.size }
                if (largestCluster != null && largestCluster.centroid != null && largestCluster.locations.size >= minArticlesPerMapCluster) {
                    largestClusterArticles.addAll(largestCluster.locations.map { it.displayTitle }.take(minArticlesPerMapCluster))
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

    private suspend fun getEditingStats(dateRange: YearInReviewDateRange): YearInReviewEditingStats? {
        if (!AccountUtil.isLoggedIn) {
            return null
        }
        val wikiSite = WikipediaApp.instance.wikiSite
        val userInfoResponse = ServiceFactory.get(wikiSite).getLocalAndGlobalUserInfo()
        return coroutineScope {
            val editCount = async { getEditCount(dateRange, userInfoResponse.query?.globalUserInfo?.id ?: 0) }
            val editedPageViews = async { getEditedPageViews(wikiSite, userInfoResponse.query?.userInfo?.id ?: 0) }
            YearInReviewEditingStats(
                userEditsCount = editCount.await(),
                userEditsViewedTimes = editedPageViews.await()
            )
        }
    }

    private suspend fun getEditCount(dateRange: YearInReviewDateRange, globalUserId: Int): Int {
        return try {
            ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getEditsPerGlobalUserMonthly(
                    globalUserId,
                    DateUtil.getYMDDateString(dateRange.start),
                    DateUtil.getYMDDateString(dateRange.endInclusive)
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

    private suspend fun getTopVisitedCategories(dateRange: YearInReviewDateRange): List<String> {
        val categories = categoryDao.getTopCategoriesByYear(year = dateRange.endInclusive.year, limit = maxTopCategory * 10)
            .map { StringUtil.removeNamespace(it.title) }
            .filter { it.isNotBlank() }
        val (categoriesWithTwoSpaces, remainingCategories) = categories.partition { category -> category.count { it == ' ' } >= 2 }
        return (categoriesWithTwoSpaces + remainingCategories).take(maxTopCategory)
    }
}
