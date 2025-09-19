package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.R
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
        _uiScreenListState.value = UiState.Success(
            data = listOf(nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
        )
    }
    private var _uiScreenListState = MutableStateFlow<UiState<List<YearInReviewScreenData>>>(UiState.Loading)
    val uiScreenListState = _uiScreenListState.asStateFlow()

    private var _canShowSurvey: Boolean = false
    var canShowSurvey: Boolean
        get() = _canShowSurvey && !Prefs.yirSurveyShown
        set(value) {
            _canShowSurvey = value
        }

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        viewModelScope.launch(handler) {

            _uiScreenListState.value = UiState.Loading

            // TODO: handle remote config to show numbers, maybe grab generic content from the config.
            val remoteConfig = RemoteConfig.config

            // TODO: content TBD
            val savedArticlesCountJob = async {
                val savedArticlesCount = AppDatabase.instance.readingListPageDao().getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                if (savedArticlesCount >= MINIMUM_SAVED_ARTICLE_COUNT) {
                    val savedArticlesTitles = AppDatabase.instance.readingListPageDao().getLastestArticleTitles(MINIMUM_SAVED_ARTICLE_COUNT)
                        .map { StringUtil.fromHtml(it).toString() }
                    YearInReviewScreenData.StandardScreen(
                        animatedImageResource = R.drawable.wyir_block_5_resize,
                        staticImageResource = R.drawable.personal_slide_01,
                        headlineText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_headline,
                            savedArticlesCount,
                            savedArticlesCount
                        ),
                        bodyText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_bodytext,
                            savedArticlesCount,
                            savedArticlesCount,
                            savedArticlesTitles[0],
                            savedArticlesTitles[1],
                            savedArticlesTitles[2]
                        )
                    )
                } else {
                    nonEnglishCollectiveReadCountData
                }
            }

            val readCountJob = async {
                val readCount = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountBetween(startTimeInMillis, endTimeInMillis)
                if (readCount >= MINIMUM_READ_COUNT) {
                    val readCountApiTitles = AppDatabase.instance.historyEntryDao().getLastestArticleTitles(MINIMUM_READ_COUNT)
                        .map { StringUtil.fromHtml(it).toString() }
                    YearInReviewScreenData.StandardScreen(
                        animatedImageResource = R.drawable.wyir_block_5_resize,
                        staticImageResource = R.drawable.personal_slide_01,
                        headlineText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_headline,
                            readCount,
                            readCount
                        ),
                        bodyText = WikipediaApp.instance.resources.getQuantityString(
                            R.plurals.year_in_review_read_count_bodytext,
                            readCount,
                            readCount,
                            readCountApiTitles[0],
                            readCountApiTitles[1],
                            readCountApiTitles[2]
                        )
                    )
                } else {
                    nonEnglishCollectiveReadCountData
                }
            }

            // TODO: think about the actual data to show.

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
                    val editCount = impact.totalUserEditCount
                    if (editCount >= MINIMUM_EDIT_COUNT) {
                        YearInReviewScreenData.StandardScreen(
                            animatedImageResource = R.drawable.wyir_bytes,
                            staticImageResource = R.drawable.english_slide_05,
                            headlineText = WikipediaApp.instance.resources.getQuantityString(
                                R.plurals.year_in_review_edit_count_headline,
                                editCount,
                                editCount
                            ),
                            bodyText = WikipediaApp.instance.resources.getQuantityString(
                                R.plurals.year_in_review_edit_count_bodytext,
                                editCount,
                                editCount
                            )
                        )
                    } else {
                        // TODO: show generic content
                        nonEnglishCollectiveEditCountData
                    }
                } else {
                    // TODO: show non-logged in user content
                    nonEnglishCollectiveEditCountData
                }
            }

            // TODO: make sure return enough slides here
            _uiScreenListState.value = UiState.Success(
                data = listOf(readCountJob.await(), savedArticlesCountJob.await(), impactDataJob.await())
            )
        }
    }

    companion object {
        private const val MINIMUM_READ_COUNT = 3
        private const val MINIMUM_SAVED_ARTICLE_COUNT = 3
        private const val MINIMUM_EDIT_COUNT = 1

        val getStartedData = YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.year_in_review_block_10_resize,
            staticImageResource = R.drawable.personal_slide_00,
            headlineText = R.string.year_in_review_get_started_headline,
            bodyText = R.string.year_in_review_get_started_bodytext,
        )

        val nonEnglishCollectiveReadCountData = YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.wyir_puzzle_3,
            staticImageResource = R.drawable.non_english_slide_01,
            headlineText = R.string.year_in_review_non_english_collective_readcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_readcount_bodytext,
        )

        val nonEnglishCollectiveEditCountData = YearInReviewScreenData.StandardScreen(
            animatedImageResource = R.drawable.wyir_puzzle_2_v5,
            staticImageResource = R.drawable.english_slide_01_and_non_english_slide_05,
            headlineText = R.string.year_in_review_non_english_collective_editcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_editcount_bodytext,
        )
    }
}
