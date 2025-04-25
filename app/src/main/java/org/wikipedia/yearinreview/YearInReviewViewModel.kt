package org.wikipedia.yearinreview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import org.wikipedia.util.log.L
import java.util.Calendar
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class YearInReviewViewModel(appContext: Application) : AndroidViewModel(appContext) {
    private val wikiAppContext = appContext
    private var _uiScreenListState = MutableStateFlow(Resource<List<YearInReviewScreenData>>())
    val uiScreenListState: StateFlow<Resource<List<YearInReviewScreenData>>> = _uiScreenListState.asStateFlow()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
    val startTime = "$currentYear-01-01T00:00:00Z"
    val endTime = "$currentYear-12-31T23:59:59Z"
    val startTimeInMillis = Instant.parse(startTime).toEpochMilliseconds()
    val endTimeInMillis = Instant.parse(endTime).toEpochMilliseconds()

    val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiScreenListState.value = Resource.Success(
            data = listOf(nonEnglishCollectiveReadCountData, nonEnglishCollectiveEditCountData)
        )
    }

    fun fetchPersonalizedData() {
        val personalizedStatistics = YearInReviewStatistics()
        viewModelScope.launch(handler) {
            _uiScreenListState.value = Resource.Loading()
            val readCountJob = async {
                personalizedStatistics.readCount = AppDatabase.instance.historyEntryDao().getHistoryCount(startTimeInMillis, endTimeInMillis)
                if (personalizedStatistics.readCount < 3) throw IllegalStateException("0")
                personalizedStatistics.readCountApiTitles = AppDatabase.instance.historyEntryDao().getApiTitles()
                readCountData.headLineText = wikiAppContext.getString(
                    R.string.year_in_review_read_count_headline,
                    personalizedStatistics.readCount.toString()
                )
                readCountData.bodyText = wikiAppContext.getString(
                    R.string.year_in_review_read_count_bodytext,
                    personalizedStatistics.readCount.toString(),
                    personalizedStatistics.readCountApiTitles[0],
                    personalizedStatistics.readCountApiTitles[1],
                    personalizedStatistics.readCountApiTitles[2]
                )
                return@async readCountData
            }
            val editCountJob = async {
                val homeSiteCall = async {
                    ServiceFactory.get(WikipediaApp.instance.wikiSite)
                        .getUserContribsByTimeFrame(
                            username = AccountUtil.userName,
                            maxCount = 500,
                            startDate = endTime,
                            endDate = startTime,
                            ns = null,
                            uccontinue = null
                        )
                }
                val commonsCall = async {
                    ServiceFactory.get(Constants.commonsWikiSite)
                        .getUserContribsByTimeFrame(
                            username = AccountUtil.userName,
                            maxCount = 500,
                            startDate = endTime,
                            endDate = startTime,
                            ns = null,
                            uccontinue = null
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
                            uccontinue = null
                        )
                }

                val homeSiteResponse = homeSiteCall.await()
                val commonsResponse = commonsCall.await()
                val wikidataResponse = wikidataCall.await()

                personalizedStatistics.editCount += homeSiteResponse.query?.userInfo!!.editCount
                personalizedStatistics.editCount += wikidataResponse.query?.userInfo!!.editCount
                personalizedStatistics.editCount += commonsResponse.query?.userInfo!!.editCount

                editCountData.headLineText = wikiAppContext.getString(
                    R.string.year_in_review_edit_count_headline,
                    personalizedStatistics.editCount.toString()
                )
                editCountData.bodyText = wikiAppContext.getString(
                    R.string.year_in_review_edit_count_bodytext,
                    personalizedStatistics.editCount.toString()
                )
                return@async editCountData
            }
            _uiScreenListState.value = Resource.Success(
                data = listOf(readCountJob.await(), editCountJob.await())
            )
        }
    }
}
