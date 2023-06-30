package org.wikipedia.suggestededits

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
import org.wikipedia.dataclient.ServiceFactory
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

    var blockMessageWikipedia: String? = null
    var blockMessageWikidata: String? = null
    var blockMessageCommons: String? = null

    var totalPageviews = 0L
    var totalContributions = 0
    var latestEditDate = Date()
    var latestEditStreak = 0
    var revertSeverity = 0

    fun fetchData() {
        _uiState.value = UiState.Loading()

        if (!AccountUtil.isLoggedIn) {
            _uiState.value = UiState.RequireLogin()
            return
        }

        viewModelScope.launch(handler) {
            blockMessageWikipedia = null
            blockMessageWikidata = null
            blockMessageCommons = null
            totalContributions = 0
            latestEditStreak = 0
            revertSeverity = 0

            val homeSiteCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName, 10, null) }
            val commonsCall = async { ServiceFactory.get(Constants.commonsWikiSite).getUserContributions(AccountUtil.userName, 10, null) }
            val wikidataCall = async { ServiceFactory.get(Constants.wikidataWikiSite).getUserContributions(AccountUtil.userName, 10, null) }
            val editCountsCall = async { UserContribStats.verifyEditCountsAndPauseState() }

            val homeSiteResponse = homeSiteCall.await()
            val commonsResponse = commonsCall.await()
            val wikidataResponse = wikidataCall.await()
            editCountsCall.await()

            homeSiteResponse.query?.userInfo?.let {
                if (it.isBlocked) {
                    blockMessageWikipedia = ThrowableUtil.getBlockMessageHtml(it, WikipediaApp.instance.wikiSite)
                }
            }
            wikidataResponse.query?.userInfo?.let {
                if (it.isBlocked) {
                    blockMessageWikidata = ThrowableUtil.getBlockMessageHtml(it, Constants.wikidataWikiSite)
                }
            }
            commonsResponse.query?.userInfo?.let {
                if (it.isBlocked) {
                    blockMessageCommons = ThrowableUtil.getBlockMessageHtml(it, Constants.commonsWikiSite)
                }
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

            totalPageviews = UserContribStats.getPageViews(wikidataResponse)

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
