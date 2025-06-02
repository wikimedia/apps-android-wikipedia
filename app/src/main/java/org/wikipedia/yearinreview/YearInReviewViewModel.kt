package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
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
        _uiScreenListState.value = Resource.Success(
            data = listOf(nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
        )
    }
    private var _uiScreenListState = MutableStateFlow(Resource<List<YearInReviewScreenData>>())
    val uiScreenListState: StateFlow<Resource<List<YearInReviewScreenData>>> = _uiScreenListState.asStateFlow()

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        val personalizedStatistics = YearInReviewStatistics()
        viewModelScope.launch(handler) {
            _uiScreenListState.value = Resource.Loading()

            val readCountJob = async {
                personalizedStatistics.readCount = AppDatabase.instance.historyEntryDao().getHistoryCount(startTimeInMillis, endTimeInMillis)
                if (personalizedStatistics.readCount >= MINIMUM_READ_COUNT) {
                    personalizedStatistics.readCountApiTitles = AppDatabase.instance.historyEntryDao().getDisplayTitles()
                        .map { StringUtil.fromHtml(it).toString() }
                    readCountData.headLineText = WikipediaApp.instance.resources.getQuantityString(
                        R.plurals.year_in_review_read_count_headline,
                        personalizedStatistics.readCount,
                        personalizedStatistics.readCount
                    )
                    readCountData.bodyText = WikipediaApp.instance.resources.getQuantityString(
                        R.plurals.year_in_review_read_count_bodytext,
                        personalizedStatistics.readCount,
                        personalizedStatistics.readCount,
                        personalizedStatistics.readCountApiTitles[0],
                        personalizedStatistics.readCountApiTitles[1],
                        personalizedStatistics.readCountApiTitles[2]
                    )
                    readCountData
                } else {
                    nonEnglishCollectiveReadCountData
                }
            }

            //
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

            personalizedStatistics.editCount += homeSiteResponse.query?.userInfo!!.editCount
            personalizedStatistics.editCount += wikidataResponse.query?.userInfo!!.editCount
            personalizedStatistics.editCount += commonsResponse.query?.userInfo!!.editCount

            if (personalizedStatistics.editCount >= MINIMUM_EDIT_COUNT) {
                editCountData.headLineText = WikipediaApp.instance.resources.getQuantityString(
                    R.plurals.year_in_review_edit_count_headline,
                    personalizedStatistics.editCount,
                    personalizedStatistics.editCount
                )
                editCountData.bodyText = WikipediaApp.instance.resources.getQuantityString(
                    R.plurals.year_in_review_edit_count_bodytext,
                    personalizedStatistics.editCount,
                    personalizedStatistics.editCount
                )

                _uiScreenListState.value = Resource.Success(
                    data = listOf(readCountJob.await(), editCountData)
                )
            } else {
                _uiScreenListState.value = Resource.Success(
                    data = listOf(readCountJob.await(), nonEnglishCollectiveEditCountData)
                )
            }
        }
    }

    companion object {
        private const val MINIMUM_READ_COUNT = 3
        private const val MINIMUM_EDIT_COUNT = 1

        val getStartedData = YearInReviewScreenData(
            animatedImageResource = R.drawable.year_in_review_block_10_resize,
            staticImageResource = R.drawable.personal_slide_00,
            headLineText = R.string.year_in_review_get_started_headline,
            bodyText = R.string.year_in_review_get_started_bodytext,
        )

        val readCountData = YearInReviewScreenData(
            animatedImageResource = R.drawable.wyir_block_5_resize,
            staticImageResource = R.drawable.personal_slide_01
        )

        val editCountData = YearInReviewScreenData(
            animatedImageResource = R.drawable.wyir_bytes,
            staticImageResource = R.drawable.english_slide_05

        )

        val nonEnglishCollectiveReadCountData = YearInReviewScreenData(
            animatedImageResource = R.drawable.wyir_puzzle_3,
            staticImageResource = R.drawable.non_english_slide_01 ,
            headLineText = R.string.year_in_review_non_english_collective_readcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_readcount_bodytext,
        )

        val nonEnglishCollectiveEditCountData = YearInReviewScreenData(
            animatedImageResource = R.drawable.wyir_puzzle_2_v5,
            staticImageResource = R.drawable.english_slide_01_and_non_english_slide_05,
            headLineText = R.string.year_in_review_non_english_collective_editcount_headline,
            bodyText = R.string.year_in_review_non_english_collective_editcount_bodytext,
        )
    }
}
