package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.ThrowableUtil
import java.time.temporal.ChronoUnit
import java.util.Date

class SuggestedEditsTasksFragmentViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    var blockMessage: String? = null
    var totalPageviews = 0L
    var totalContributions = 0
    var latestEditDate = Date()
    var latestEditStreak = 0
    var revertSeverity = 0

    var imageRecommendationsEnabled = false

    fun fetchData() {
        _uiState.value = UiState.Loading()
        imageRecommendationsEnabled = false

        if (!AccountUtil.isLoggedIn) {
            _uiState.value = UiState.RequireLogin()
            return
        }

        viewModelScope.launch(handler) {
            blockMessage = null
            totalContributions = 0
            latestEditStreak = 0
            revertSeverity = 0

            val homeSiteCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName!!, 10, null) }
            val homeSiteParamCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getParamInfo("query+growthtasks") }
            val commonsCall = async { ServiceFactory.get(Constants.commonsWikiSite).getUserContributions(AccountUtil.userName!!, 10, null) }
            val wikidataCall = async { ServiceFactory.get(Constants.wikidataWikiSite).getUserContributions(AccountUtil.userName!!, 10, null) }
            val editCountsCall = withContext(Dispatchers.IO) { UserContribStats.getEditCountsObservable().blockingSingle() }

            val homeSiteResponse = homeSiteCall.await()
            val commonsResponse = commonsCall.await()
            val wikidataResponse = wikidataCall.await()

            homeSiteParamCall.await().paraminfo?.modules?.let {
                if (it.isNotEmpty() && it[0].parameters.isNotEmpty()) {
                    imageRecommendationsEnabled = it[0].parameters[0].typeAsEnum.contains("image-recommendation")
                }
            }

            var blockInfo: MwServiceError.BlockInfo? = null
            when {
                wikidataResponse.query?.userInfo!!.isBlocked -> blockInfo =
                    wikidataResponse.query?.userInfo!!
                commonsResponse.query?.userInfo!!.isBlocked -> blockInfo =
                    commonsResponse.query?.userInfo!!
                homeSiteResponse.query?.userInfo!!.isBlocked -> blockInfo =
                    homeSiteResponse.query?.userInfo!!
            }
            if (blockInfo != null) {
                blockMessage = ThrowableUtil.getBlockMessageHtml(blockInfo)
            }

            totalContributions += wikidataResponse.query?.userInfo!!.editCount
            totalContributions += commonsResponse.query?.userInfo!!.editCount
            totalContributions += homeSiteResponse.query?.userInfo!!.editCount

            latestEditDate = wikidataResponse.query?.userInfo!!.latestContribDate

            if (commonsResponse.query?.userInfo!!.latestContribDate.after(latestEditDate)) {
                latestEditDate = commonsResponse.query?.userInfo!!.latestContribDate
            }

            if (homeSiteResponse.query?.userInfo!!.latestContribDate.after(latestEditDate)) {
                latestEditDate = homeSiteResponse.query?.userInfo!!.latestContribDate
            }

            latestEditStreak = getEditStreak(
                wikidataResponse.query!!.userContributions +
                        commonsResponse.query!!.userContributions +
                        homeSiteResponse.query!!.userContributions
            )
            revertSeverity = UserContribStats.getRevertSeverity()

            withContext(Dispatchers.IO) {
                totalPageviews = UserContribStats.getPageViewsObservable(wikidataResponse).blockingSingle()
            }

            _uiState.value = UiState.Success()
        }
    }

    private fun getEditStreak(contributions: List<UserContribution>): Int {
        if (contributions.isEmpty()) {
            return 0
        }
        val dates = contributions.map { it.parsedDateTime.toLocalDate() }
            .toSortedSet(Comparator.reverseOrder())
        return dates.asSequence()
            .zipWithNext { date1, date2 -> date2.until(date1, ChronoUnit.DAYS) }
            .takeWhile { it == 1L }
            .count()
    }

    open class UiState {
        class Loading : UiState()
        class RequireLogin : UiState()
        class Success : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
