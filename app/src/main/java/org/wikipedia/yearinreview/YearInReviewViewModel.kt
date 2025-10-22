package org.wikipedia.yearinreview

import android.graphics.Bitmap
import android.location.Geocoder
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.GeoUtil.LocationClusterer
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class YearInReviewViewModel() : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiScreenListState.value = UiState.Error(throwable)
    }
    private var _uiScreenListState = MutableStateFlow<UiState<List<YearInReviewScreenData>>>(UiState.Loading)
    val uiScreenListState = _uiScreenListState.asStateFlow()

    var screenshotHeaderBitmap = createBitmap(1, 1)

    var slideViewedCount = 1

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        viewModelScope.launch(handler) {

            _uiScreenListState.value = UiState.Loading

            val remoteConfig = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration().commonv1?.getYirForYear(YIR_YEAR)!!
            val dataStartInstant = remoteConfig.dataStartDate.toInstant(ZoneOffset.UTC)
            val dataEndInstant = remoteConfig.dataEndDate.toInstant(ZoneOffset.UTC)
            val dataStartMillis = dataStartInstant.toEpochMilli()
            val dataEndMillis = dataEndInstant.toEpochMilli()

            var pagesWithCoordinates = AppDatabase.instance.historyEntryWithImageDao().getEntriesWithCoordinates(256, dataStartMillis, dataEndMillis)
                .distinctBy { it.apiTitle }

            val yearInReviewModelMap = Prefs.yearInReviewModelData.toMutableMap()

            if (yearInReviewModelMap[YIR_YEAR] == null) {
                val totalSavedArticlesCount = async {
                    AppDatabase.instance.readingListPageDao()
                        .getTotalLocallySavedPagesBetween(dataStartMillis, dataEndMillis) ?: 0
                }
                val randomSavedArticleTitles = async {
                    AppDatabase.instance.readingListPageDao()
                        .getRandomPageTitlesBetween(MIN_SAVED_ARTICLES, dataStartMillis, dataEndMillis)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val readCountForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getDistinctEntriesCountBetween(dataStartMillis, dataEndMillis)
                }

                val topVisitedArticlesForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getTopVisitedEntriesBetween(MAX_TOP_ARTICLES, dataStartMillis, dataEndMillis)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val totalReadingTimeMinutes = async {
                    AppDatabase.instance.historyEntryWithImageDao()
                        .getTimeSpentBetween(dataStartMillis, dataEndMillis) / 60
                }

                val topVisitedCategoryForTheYear = async {
                    val categories = AppDatabase.instance.categoryDao().getTopCategoriesByYear(year = YIR_YEAR, limit = MAX_TOP_CATEGORY * 10)
                        .map { StringUtil.removeNamespace(it.title) }
                    val categoriesWithTwoSpaces = categories.filter { it.count { c -> c == ' ' } >= 2 }
                    val remainingCategories = categories.filter { it.count { c -> c == ' ' } < 2 }
                    categoriesWithTwoSpaces.plus(remainingCategories)
                        .take(MAX_TOP_CATEGORY)
                }

                val impactDataJob = async {
                    if (AccountUtil.isLoggedIn) {
                        val wikiSite = WikipediaApp.instance.wikiSite
                        val now = Instant.now().epochSecond
                        val impact: GrowthUserImpact
                        val impactLastResponseBodyMap = Prefs.impactLastResponseBody.toMutableMap()
                        val impactResponse = impactLastResponseBodyMap[wikiSite.languageCode]
                        if (impactResponse.isNullOrEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.HOURS.toSeconds(12)) {
                            val userId =
                                ServiceFactory.get(wikiSite).getUserInfo().query?.userInfo?.id!!
                            impact = ServiceFactory.getCoreRest(wikiSite).getUserImpact(userId)
                            impactLastResponseBodyMap[wikiSite.languageCode] =
                                JsonUtil.encodeToString(impact).orEmpty()
                            Prefs.impactLastResponseBody = impactLastResponseBodyMap
                            Prefs.impactLastQueryTime = now
                        } else {
                            impact = JsonUtil.decodeFromString(impactResponse)!!
                        }

                        val pagesResponse = ServiceFactory.get(wikiSite).getInfoByPageIdsOrTitles(
                            titles = impact.topViewedArticles.keys.joinToString(separator = "|")
                        )

                        // Transform the response to a map of PageTitle to ArticleViews
                        val pageMap = pagesResponse.query?.pages?.associate { page ->
                            val pageTitle = PageTitle(
                                text = page.title,
                                wiki = wikiSite,
                                thumbUrl = page.thumbUrl(),
                                description = page.description,
                                displayText = page.displayTitle(wikiSite.languageCode)
                            )
                            pageTitle to impact.topViewedArticles[pageTitle.text]!!
                        } ?: emptyMap()

                        impact.topViewedArticlesWithPageTitle = pageMap
                        impact
                    } else {
                        GrowthUserImpact()
                    }
                }

                var editCount = 0
                if (AccountUtil.isLoggedIn) {
                    val homeSiteCall = async {
                        ServiceFactory.get(WikipediaApp.instance.wikiSite)
                            .getUserContribsByTimeFrame(
                                username = AccountUtil.userName,
                                maxCount = 500,
                                startDate = dataEndInstant,
                                endDate = dataStartInstant
                            )
                    }
                    val commonsCall = async {
                        ServiceFactory.get(Constants.commonsWikiSite)
                            .getUserContribsByTimeFrame(
                                username = AccountUtil.userName,
                                maxCount = 500,
                                startDate = dataEndInstant,
                                endDate = dataStartInstant
                            )
                    }
                    val wikidataCall = async {
                        ServiceFactory.get(Constants.wikidataWikiSite)
                            .getUserContribsByTimeFrame(
                                username = AccountUtil.userName,
                                maxCount = 500,
                                startDate = dataEndInstant,
                                endDate = dataStartInstant,
                                ns = 0,
                            )
                    }

                    val homeSiteResponse = homeSiteCall.await()
                    val commonsResponse = commonsCall.await()
                    val wikidataResponse = wikidataCall.await()

                    editCount += homeSiteResponse.query?.userInfo!!.editCount
                    editCount += wikidataResponse.query?.userInfo!!.editCount
                    editCount += commonsResponse.query?.userInfo!!.editCount
                }

                val favoriteTimeToRead = async {
                    AppDatabase.instance.historyEntryDao()
                        .getFavoriteTimeToReadBetween(dataStartMillis, dataEndMillis)
                }

                val favoriteDayToRead = async {
                    AppDatabase.instance.historyEntryDao()
                        .getFavoriteDayToReadBetween(dataStartMillis, dataEndMillis)
                }

                val mostReadingMonth = async {
                    AppDatabase.instance.historyEntryDao()
                        .getMostReadingMonthBetween(dataStartMillis, dataEndMillis)
                }

                val favoriteTimeToReadHour = favoriteTimeToRead.await() ?: 0

                val favoriteDayToReadIndex = favoriteDayToRead.await()?.let {
                    if (it == 0) 7 else it
                } ?: 1

                val mostReadingMonthIndex = mostReadingMonth.await() ?: 1

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
                                largestClusterCountryName = results.first().countryName
                            }
                            pagesWithCoordinates = largestCluster.locations.plus(pagesWithCoordinates.minus(largestCluster.locations))
                        }
                    } catch (_: IOException) {
                        // could be thrown by Geocoder, and safe to ignore.
                    }
                }

                yearInReviewModelMap[YIR_YEAR] = YearInReviewModel(
                    totalReadingTimeMinutes = totalReadingTimeMinutes.await(),
                    localSavedArticlesCount = totalSavedArticlesCount.await(),
                    localReadingArticlesCount = readCountForTheYear.await(),
                    localSavedArticles = randomSavedArticleTitles.await(),
                    localTopVisitedArticles = topVisitedArticlesForTheYear.await(),
                    localTopCategories = topVisitedCategoryForTheYear.await(),
                    favoriteTimeToRead = favoriteTimeToReadHour,
                    favoriteDayToRead = favoriteDayToReadIndex,
                    favoriteMonthDidMostReading = mostReadingMonthIndex,
                    largestClusterLocation = Pair(largestClusterLatitude, largestClusterLongitude),
                    largestClusterTopLeft = largestClusterTopLeft,
                    largestClusterBottomRight = largestClusterBottomRight,
                    largestClusterCountryName = largestClusterCountryName,
                    largestClusterArticles = largestClusterArticles,
                    userEditsCount = editCount,
                    userEditsViewedTimes = impactDataJob.await().totalPageviewsCount,
                    isCustomIconUnlocked = editCount > 0 || Prefs.donationResults.isNotEmpty()
                )

                Prefs.yearInReviewModelData = yearInReviewModelMap
                YearInReviewSurvey.resetYearInReviewSurveyState()
            }

            val yearInReviewModel = yearInReviewModelMap[YIR_YEAR]!!

            val finalRoute = YearInReviewSlides(
                context = WikipediaApp.instance,
                currentYear = YIR_YEAR,
                isEditor = yearInReviewModel.userEditsCount > 0,
                isLoggedIn = AccountUtil.isLoggedIn,
                isEnglishWiki = WikipediaApp.instance.wikiSite.languageCode == "en",
                isFundraisingAllowed = !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty()),
                config = remoteConfig,
                pagesWithCoordinates = pagesWithCoordinates,
                yearInReviewModel = yearInReviewModel
            ).finalSlides()

            // TODO: make sure return enough slides here
            _uiScreenListState.value = UiState.Success(
                data = finalRoute
            )
        }
    }

    fun requestScreenshotHeaderBitmap(width: Int = 0, height: Int = 0): Bitmap {
        if ((screenshotHeaderBitmap.width != width || screenshotHeaderBitmap.height != height) && width > 0 && height > 0) {
            screenshotHeaderBitmap = createBitmap(width, height)
        }
        return screenshotHeaderBitmap
    }

    companion object {
        const val YIR_YEAR = 2025
        const val YIR_TAG = "yir_$YIR_YEAR"
        const val MAX_EDITED_TIMES = 500
        const val MIN_SAVED_ARTICLES = 3
        const val MAX_TOP_ARTICLES = 5
        const val MIN_TOP_CATEGORY = 3
        const val MAX_TOP_CATEGORY = 5
        const val MIN_READING_ARTICLES = 5
        const val MIN_READING_MINUTES = 1
        const val MIN_ARTICLES_PER_MAP_CLUSTER = 2
        const val MAX_ARTICLES_ON_MAP = 32
        const val MIN_SLIDES_BEFORE_SURVEY = 2

        // Whether Year-in-Review should be accessible at all.
        // (different from the user enabling/disabling it in Settings.)
        val isAccessible get(): Boolean {
            if (Prefs.isShowDeveloperSettingsEnabled) {
                return true
            }
            val config = RemoteConfig.config.commonv1?.getYirForYear(YIR_YEAR)
            val now = LocalDateTime.now()
            return (config != null &&
                    !config.hideCountryCodes.contains(GeoUtil.geoIPCountry) &&
                    now.isAfter(config.activeStartDate) &&
                    now.isBefore(config.activeEndDate))
        }

        val isCustomIconAllowed get(): Boolean {
            return Prefs.yearInReviewModelData[YIR_YEAR]?.isCustomIconUnlocked == true
        }
    }
}
