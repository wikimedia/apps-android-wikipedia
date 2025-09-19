package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class YearInReviewViewModel() : ViewModel() {
    private val currentYear = LocalDate.now().year
    private val startTime: Instant = LocalDateTime.of(currentYear, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC)
    private val endTime: Instant = LocalDateTime.of(currentYear, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC)
    private val startTimeInMillis = startTime.toEpochMilli()
    private val endTimeInMillis = endTime.toEpochMilli()
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiScreenListState.value = UiState.Success(
            data = listOf(testGeo, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
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

            val readCountJob = async {
                val readCount = AppDatabase.instance.historyEntryDao().getDistinctEntriesBetween(startTimeInMillis, endTimeInMillis)
                if (readCount >= MINIMUM_READ_COUNT) {
                    val readCountApiTitles = AppDatabase.instance.historyEntryDao().getDisplayTitles()
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

            if (editCount >= MINIMUM_EDIT_COUNT) {
                val editCountData = YearInReviewScreenData.StandardScreen(
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
                _uiScreenListState.value = UiState.Success(
                    data = listOf(testGeo, readCountJob.await(), editCountData, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
                )
            } else {
                _uiScreenListState.value = UiState.Success(
                    data = listOf(testGeo, readCountJob.await(), nonEnglishCollectiveEditCountData, nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
                )
            }
        }
    }

    companion object {
        private const val MINIMUM_READ_COUNT = 3
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

        val testGeo = YearInReviewScreenData.GeoScreen(
            coordinates = mapOf()
        )
    }
}
