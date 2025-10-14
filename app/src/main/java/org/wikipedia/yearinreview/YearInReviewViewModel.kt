package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class YearInReviewViewModel() : ViewModel() {
    private val startTime: Instant = LocalDateTime.of(YIR_YEAR, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
    private val endTime: Instant = LocalDateTime.of(YIR_YEAR, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC)
    private val startTimeInMillis = startTime.toEpochMilli()
    private val endTimeInMillis = endTime.toEpochMilli()
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiScreenListState.value = UiState.Error(throwable)
    }
    private var _uiScreenListState = MutableStateFlow<UiState<List<YearInReviewScreenData>>>(UiState.Loading)
    val uiScreenListState = _uiScreenListState.asStateFlow()

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        viewModelScope.launch(handler) {

            _uiScreenListState.value = UiState.Loading

            val remoteConfig = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration().commonv1?.getYirForYear(YIR_YEAR)!!

            val yearInReviewModelMap = Prefs.yearInReviewModelData.toMutableMap()

            if (yearInReviewModelMap[YIR_YEAR] == null) {
                val now =
                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val totalSavedArticlesCount = async {
                    AppDatabase.instance.readingListPageDao()
                        .getTotalLocallySavedPagesBetween(startTimeInMillis, endTimeInMillis) ?: 0
                }
                val randomSavedArticleTitles = async {
                    AppDatabase.instance.readingListPageDao()
                        .getRandomPageTitlesBetween(MIN_SAVED_ARTICLES, startTimeInMillis, endTimeInMillis)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val readCountForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                }

                val topVisitedArticlesForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getTopVisitedEntriesBetween(MAX_TOP_ARTICLES, startTimeInMillis, endTimeInMillis)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val totalTimeSpent = async {
                    AppDatabase.instance.historyEntryWithImageDao()
                        .getTimeSpentBetween(startTimeInMillis, endTimeInMillis)
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

                val homeSiteCall = async {
                    ServiceFactory.get(WikipediaApp.instance.wikiSite)
                        .getUserContribsByTimeFrame(
                            username = AccountUtil.userName,
                            maxCount = 500,
                            startDate = endTime,
                            endDate = startTime
                        )
                }
                val commonsCall = async {
                    ServiceFactory.get(Constants.commonsWikiSite)
                        .getUserContribsByTimeFrame(
                            username = AccountUtil.userName,
                            maxCount = 500,
                            startDate = endTime,
                            endDate = startTime
                        )
                }
                val wikidataCall = async {
                    ServiceFactory.get(Constants.wikidataWikiSite)
                        .getUserContribsByTimeFrame(
                            username = AccountUtil.userName,
                            maxCount = 500,
                            startDate = endTime,
                            endDate = startTime,
                            ns = 0,
                        )
                }

                val homeSiteResponse = homeSiteCall.await()
                val commonsResponse = commonsCall.await()
                val wikidataResponse = wikidataCall.await()

                var editCount = homeSiteResponse.query?.userInfo!!.editCount
                editCount += wikidataResponse.query?.userInfo!!.editCount
                editCount += commonsResponse.query?.userInfo!!.editCount

                val favoriteTimeToRead = async {
                    AppDatabase.instance.historyEntryDao()
                        .getFavoriteTimeToReadBetween(startTimeInMillis, endTimeInMillis)
                }

                val favoriteDayToRead = async {
                    AppDatabase.instance.historyEntryDao()
                        .getFavoriteDayToReadBetween(startTimeInMillis, endTimeInMillis)
                }

                val mostReadingMonth = async {
                    AppDatabase.instance.historyEntryDao()
                        .getMostReadingMonthBetween(startTimeInMillis, endTimeInMillis)
                }

                val favoriteTimeToReadHour = favoriteTimeToRead.await() ?: 0

                val favoriteDayToReadIndex = favoriteDayToRead.await()?.let {
                    if (it == 0) 7 else it
                } ?: 1

                val mostReadingMonthIndex = mostReadingMonth.await() ?: 1

                yearInReviewModelMap[YIR_YEAR] = YearInReviewModel(
                    localReadingTimePerMinute = totalTimeSpent.await(),
                    localSavedArticlesCount = totalSavedArticlesCount.await(),
                    localReadingArticlesCount = readCountForTheYear.await(),
                    localSavedArticles = randomSavedArticleTitles.await(),
                    localTopVisitedArticles = topVisitedArticlesForTheYear.await(),
                    localTopCategories = topVisitedCategoryForTheYear.await(),
                    favoriteTimeToRead = favoriteTimeToReadHour,
                    favoriteDayToRead = favoriteDayToReadIndex,
                    favoriteMonthDidMostReading = mostReadingMonthIndex,
                    closestLocation = Pair(0.0, 0.0),
                    closestArticles = emptyList(),
                    userEditsCount = editCount,
                    userEditsViewedTimes = impactDataJob.await().totalPageviewsCount
                )

                Prefs.yearInReviewModelData = yearInReviewModelMap
            }

            val yearInReviewModel = yearInReviewModelMap[YIR_YEAR]!!

            val finalRoute = YearInReviewSlides(
                context = WikipediaApp.instance,
                currentYear = YIR_YEAR,
                isEditor = yearInReviewModel.userEditsCount > 0,
                isLoggedIn = AccountUtil.isLoggedIn,
                isEnglishWiki = WikipediaApp.instance.wikiSite.languageCode == "en",
                isFundraisingDisabled = remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty()),
                config = remoteConfig,
                yearInReviewModel = yearInReviewModel
            ).finalSlides()

            // TODO: make sure return enough slides here
            _uiScreenListState.value = UiState.Success(
                data = finalRoute
            )
        }
    }

    companion object {
        const val YIR_YEAR = 2025
        const val MAX_EDITED_TIMES = 500
        const val MIN_SAVED_ARTICLES = 3
        const val MAX_TOP_ARTICLES = 5
        const val MIN_TOP_CATEGORY = 3
        const val MAX_TOP_CATEGORY = 5
        const val MIN_READING_ARTICLES = 5
        const val MIN_READING_MINUTES = 1
        const val MIN_READING_PATTERNS_ARTICLES = 5
    }
}
