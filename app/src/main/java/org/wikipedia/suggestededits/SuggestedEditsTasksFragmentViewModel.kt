package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.settings.Prefs
import org.wikipedia.usercontrib.UserContribStats
import org.wikipedia.util.Resource
import org.wikipedia.util.ThrowableUtil
import java.time.Instant
import java.time.temporal.ChronoUnit

class SuggestedEditsTasksFragmentViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Unit>())
    val uiState = _uiState.asStateFlow()

    private var clientJob: Job? = null

    var blockMessageWikipedia: String? = null
    var blockMessageWikidata: String? = null
    var blockMessageCommons: String? = null

    var totalContributions = 0
    var homeContributions = 0

    var wikiSupportsImageRecommendations = false
    var allowToPatrolEdits = false

    fun fetchData() {
        _uiState.value = Resource.Loading()
        wikiSupportsImageRecommendations = false

        if (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount) {
            _uiState.value = RequireLogin()
            return
        }
        clientJob?.cancel()
        clientJob = viewModelScope.launch(handler) {
            blockMessageWikipedia = null
            blockMessageWikidata = null
            blockMessageCommons = null
            totalContributions = 0

            val homeSiteCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserContributions(AccountUtil.userName, 50, null, null) }
            // val homeSiteParamCall = async { ServiceFactory.get(WikipediaApp.instance.wikiSite).getParamInfo("query+growthtasks") }
            val commonsCall = async { ServiceFactory.get(Constants.commonsWikiSite).getUserContributions(AccountUtil.userName, 10, null, null) }
            val wikidataCall = async { ServiceFactory.get(Constants.wikidataWikiSite).getUserContributions(AccountUtil.userName, 10, 0, null) }

            val homeSiteResponse = homeSiteCall.await()
            val commonsResponse = commonsCall.await()
            val wikidataResponse = wikidataCall.await()

            // Logic for checking whether the wiki has image recommendations enabled
            // (in case we need to rely on it in the future)
            /*
            homeSiteParamCall.await().paraminfo?.modules?.let {
                if (it.isNotEmpty() && it[0].parameters.isNotEmpty()) {
                    imageRecommendationsEnabled = it[0].parameters[0].typeAsEnum.contains("image-recommendation")
                }
            }
             */
            wikiSupportsImageRecommendations = true

            homeSiteResponse.query?.userInfo?.let {
                // T371442: In the case of Igbo Wikipedia, allow patrolling if the user has 500 or more edits, and 30 days of tenure.
                // For all other wikis, allow patrolling if the user has rollback rights or is an admin.
                if (WikipediaApp.instance.wikiSite.languageCode == "ig") {
                    allowToPatrolEdits = it.editCount >= 500 && it.registrationDate.toInstant().plus(30, ChronoUnit.DAYS).isBefore(Instant.now())
                } else {
                    allowToPatrolEdits = it.rights.contains("rollback") || it.groups().contains("sysop")
                }

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

            homeContributions = homeSiteResponse.query?.userInfo!!.editCount
            totalContributions += wikidataResponse.query?.userInfo!!.editCount
            totalContributions += commonsResponse.query?.userInfo!!.editCount
            totalContributions += homeContributions

            val totalContributionsList = homeSiteResponse.query!!.userContributions +
                    wikidataResponse.query!!.userContributions +
                    commonsResponse.query!!.userContributions

            UserContribStats.verifyEditCountsAndPauseState(totalContributionsList)

            if (Prefs.overrideSuggestedEditContribution > 0) {
                totalContributions = Prefs.overrideSuggestedEditContribution
            }

            _uiState.value = Resource.Success(Unit)
        }
    }

    class RequireLogin : Resource<Unit>()
}
