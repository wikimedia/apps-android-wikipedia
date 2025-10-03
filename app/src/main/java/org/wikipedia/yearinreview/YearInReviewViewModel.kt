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
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class YearInReviewViewModel() : ViewModel() {
    private val currentYear = LocalDate.now().year
    private val startTime: Instant = LocalDateTime.of(currentYear, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
    private val endTime: Instant = LocalDateTime.of(currentYear, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC)
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

            val yearInReviewModelMap = Prefs.yearInReviewModelData.toMutableMap()

            if (yearInReviewModelMap[currentYear] == null) {
                val now =
                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val yearInMillis = TimeUnit.DAYS.toMillis(365)
                val yearAgo = now - yearInMillis

                // TODO: handle remote config to show numbers, maybe grab generic content from the config.
                val remoteConfig = RemoteConfig.config

                val latestArticleTitlesFromSaved = async {
                    AppDatabase.instance.readingListPageDao()
                        .getLatestArticleTitles(MINIMUM_SAVED_ARTICLE_COUNT)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val readCountForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                }

                val topVisitedArticlesForTheYear = async {
                    AppDatabase.instance.historyEntryDao()
                        .getTopVisitedEntriesSince(MAX_TOP_ARTICLES, yearAgo)
                        .map { StringUtil.fromHtml(it).toString() }
                }

                val totalTimeSpent = async {
                    AppDatabase.instance.historyEntryWithImageDao()
                        .getTimeSpentSinceTimeStamp(yearAgo)
                }

                val topVisitedCategoryForTheYear = async {
                    AppDatabase.instance.categoryDao().getTopCategoriesByYear(year = currentYear, limit = MAX_TOP_CATEGORY)
                        .map { StringUtil.removeNamespace(it.title) }
                }

                val impactDataJob = async {
                    if (AccountUtil.isLoggedIn) {
                        val wikiSite = WikipediaApp.instance.wikiSite
                        val now = Instant.now().epochSecond
                        val impact: GrowthUserImpact
                        val impactLastResponseBodyMap = Prefs.impactLastResponseBody.toMutableMap()
                        val impactResponse = impactLastResponseBodyMap[wikiSite.languageCode]
                        if (impactResponse.isNullOrEmpty() || abs(now - Prefs.impactLastQueryTime) > TimeUnit.HOURS.toSeconds(
                                12
                            )
                        ) {
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

                yearInReviewModelMap[currentYear] = YearInReviewModel(
                    enReadingTimePerHour = 0L, // TODO: remote config
                    enPopularArticles = listOf("Dog", "Cat", "Bear", "Bird", "Tiger"), // TODO: remote config
                    enEditsCount = 0L, // TODO: remote config
                    enBytesAddedCount = 0L, // TODO: remote config
                    availableLanguages = 0, // TODO: remote config
                    globalTotalArticles = 0L, // TODO: remote config
                    globalEditsCount = 0L, // TODO: remote config
                    globalReadingArticlesCount = 0, // TODO: remote config
                    globalEditsPerMinute = 0, // TODO: remote config
                    appArticlesViewedTimes = 0L, // TODO: remote config
                    appArticlesSavedTimes = 0L, // TODO: remote config
                    appsEditsCount = 0L, // TODO: remote config
                    localReadingTimePerMinute = totalTimeSpent.await(),
                    localReadingArticlesCount = readCountForTheYear.await(),
                    localReadingRank = "50%", // TODO: compare with the total reading hours
                    localSavedArticles = latestArticleTitlesFromSaved.await(),
                    localTopVisitedArticles = topVisitedArticlesForTheYear.await(),
                    localTopCategories = topVisitedCategoryForTheYear.await(),
                    favoriteTimeToRead = "Evening",
                    favoriteDayToRead = "Saturday",
                    favoriteMonthDidMostReading = "March",
                    closestLocation = Pair(0.0, 0.0),
                    closestArticles = emptyList(),
                    userEditsCount = editCount,
                    userEditsViewedTimes = impactDataJob.await().totalPageviewsCount
                )

                Prefs.yearInReviewModelData = yearInReviewModelMap
            }

            val yearInReviewModel = yearInReviewModelMap[currentYear]!!

            val finalRoute = YearInReviewSlides(
                context = WikipediaApp.instance,
                currentYear = currentYear,
                isEditor = yearInReviewModel.userEditsCount > 0,
                isLoggedIn = AccountUtil.isLoggedIn,
                isEnglishWiki = WikipediaApp.instance.wikiSite.languageCode == "en",
                yearInReviewModel = yearInReviewModel
            ).finalSlides()

            // TODO: make sure return enough slides here
            _uiScreenListState.value = UiState.Success(
                data = finalRoute
            )
        }
    }

    companion object {
        private const val MINIMUM_SAVED_ARTICLE_COUNT = 3
        private const val MINIMUM_EDIT_COUNT = 1
        const val MAX_TOP_ARTICLES = 5
        const val MIN_TOP_CATEGORY = 3
        const val MAX_TOP_CATEGORY = 5
    }
}
