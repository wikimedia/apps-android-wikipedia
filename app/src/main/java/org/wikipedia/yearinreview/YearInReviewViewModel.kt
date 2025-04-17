package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    val statusAndResultsMap: MutableMap<String, Resource<YearInReviewTextData>> = mutableMapOf()
    private val _masterMap = MutableStateFlow(statusAndResultsMap)
    val masterMap: StateFlow<MutableMap<String, Resource<YearInReviewTextData>>> = _masterMap.asStateFlow()
    val mutex = Mutex()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
    val startTime = "$currentYear-01-01T00:00:00Z"
    val endTime = "$currentYear-12-31T23:59:59Z"
    val startTimeInMillis = Instant.parse(startTime).toEpochMilliseconds()
    val endTimeInMillis = Instant.parse(endTime).toEpochMilliseconds()
    var totalContributions = 0

    val personalizedScreenList = listOf(
        readCountData, editCountData)

    init {
        getOnboarding()
        getReads()
        getEdits()
    }

    fun handler(
        mapState: MutableStateFlow<MutableMap<String, Resource<YearInReviewTextData>>>,
        key: String,
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { coroutineContext, throwable ->
            viewModelScope.launch {
                mutex.withLock {
                    mapState.update { currentMap ->
                        currentMap.toMutableMap().apply {
                            put(key, Resource.Error(throwable))
                        }
                    }
                }
            }
        }
    }

    private fun getOnboarding(): Job {
        return viewModelScope.launch(handler(_masterMap, PersonalizedJobID.ONBOARDING.name)) {
            mutex.withLock {
                _masterMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        put(
                            PersonalizedJobID.ONBOARDING.name,
                            Resource.Success(
                                data = YearInReviewTextData(
                                    headLineText = "",
                                    bodyText = ""
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getReads(): Job {
        return viewModelScope.launch(handler(_masterMap, PersonalizedJobID.READ_COUNT.name)) {
            val titleList = AppDatabase.instance.historyEntryDao().getApiTitles()
            val historyCount = AppDatabase.instance.historyEntryDao()
                .getHistoryCount(startTimeInMillis, endTimeInMillis)
            mutex.withLock {
                _masterMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        put(
                            PersonalizedJobID.READ_COUNT.name,
                            Resource.Success(
                                data = YearInReviewTextData(
                                    headLineText = historyCount.toString(),
                                    bodyText = buildArticleTitleString(titleList, historyCount)
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getEdits(): Job {
        return viewModelScope.launch(handler(_masterMap, PersonalizedJobID.EDIT_COUNT.name)) {
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

            mutex.withLock {
                _masterMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        put(
                            PersonalizedJobID.EDIT_COUNT.name,
                            Resource.Success(
                                data = YearInReviewTextData(
                                    headLineText = totalContributions.toString(),
                                    bodyText = totalContributions.toString()
                                )
                            )
                        )
                    }
                }
            }
        }
    }
    /* TODO: Handle plural vs singular (e.g. 5 articles vs 1 article) */
    private fun buildArticleTitleString(titleList: List<String>, historyCount: Int): String {
        var baseString = "$historyCount articles, including "
        val customSeparator = "and "
        when (titleList.size) {
            1 -> baseString += titleList.joinToString(".").replace("_", " ")
            2 -> baseString += titleList.joinToString(separator = " and ", postfix = ".").replace("_", " ")
            3 -> baseString += titleList.joinToString(separator = ", ", postfix = ".") {
                title -> if (titleList.indexOf(title) == 2) customSeparator.plus(title) else title }.replace("_", " ")
            else -> baseString = "$historyCount articles."
        }
        return baseString
    }
}
