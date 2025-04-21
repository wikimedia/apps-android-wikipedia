package org.wikipedia.yearinreview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.Resource
import java.util.Calendar
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class YearInReviewViewModel() : ViewModel() {

    private var _uiScreenListState = MutableStateFlow(Resource<List<YearInReviewScreenData>>())
    val uiScreenListState: StateFlow<Resource<List<YearInReviewScreenData>>> = _uiScreenListState.asStateFlow()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
    val startTime = "$currentYear-01-01T00:00:00Z"
    val endTime = "$currentYear-12-31T23:59:59Z"
    val startTimeInMillis = Instant.parse(startTime).toEpochMilliseconds()
    val endTimeInMillis = Instant.parse(endTime).toEpochMilliseconds()
    var totalContributions = 0

    init {
        fetchPersonalizedData()
    }

    fun fetchPersonalizedData() {
        viewModelScope.launch {
            _uiScreenListState.value = Resource.Loading()
            val readCountJob = async {
                try {
                    val titleList = AppDatabase.instance.historyEntryDao().getApiTitles()
                    val historyCount = AppDatabase.instance.historyEntryDao()
                        .getHistoryCount(startTimeInMillis, endTimeInMillis)
                    if (historyCount < 3) { throw Exception("Less than three articles read") }
                    readCountData.fetchedArgs = listOf(historyCount.toString()) + titleList
                    return@async readCountData
                } catch (e: Exception) {
                    Log.d("ReadCount Exception:", "$e")
                    return@async nonEnglishCollectiveReadCountData
                }
            }
            val editCountJob = async {
                try {
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

                    totalContributions += homeSiteResponse.query?.userInfo!!.editCount
                    totalContributions += wikidataResponse.query?.userInfo!!.editCount
                    totalContributions += commonsResponse.query?.userInfo!!.editCount

                    val fetchedData = listOf(totalContributions.toString())
                    editCountData.fetchedArgs = fetchedData
                    return@async editCountData
                } catch (e: Exception) {
                    Log.d("EditCountError", "$e")
                    return@async nonEnglishCollectiveEditCountData
                }
            }
            _uiScreenListState.value = Resource.Success(data = listOf(readCountJob.await(), editCountJob.await()))
        }
    }
}
