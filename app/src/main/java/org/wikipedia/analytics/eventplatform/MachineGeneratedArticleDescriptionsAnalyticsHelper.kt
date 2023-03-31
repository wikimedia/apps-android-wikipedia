package org.wikipedia.analytics.eventplatform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.page.PageTitle

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private const val MACHINE_GEN_DESC_SUGGESTIONS = "machineSuggestions"
    private var apiFailed = false
    val machineGeneratedDescriptionsABTest = MachineGeneratedArticleDescriptionABCTest()
    var apiOrderList = emptyList<String>()
    var displayOrderList = emptyList<String>()
    var chosenSuggestion = ""
    var isUserExperienced = false
    var isUserInExperiment = false
    private var startTime = 0L

    fun articleDescriptionEditingStart(context: Context) {
        log(context, "ArticleDescriptionEditing.start")
        startTime = System.currentTimeMillis()
    }

    fun resetTimer() {
        startTime = System.currentTimeMillis()
    }

    fun articleDescriptionEditingEnd(context: Context) {
        log(context, "ArticleDescriptionEditing.end.timeSpentMs.${System.currentTimeMillis() - startTime}")
    }

    fun logAttempt(context: Context, finalDescription: String, wasChosen: Boolean, wasModified: Boolean, title: PageTitle) {
        log(
            context, composeLogString(title) + ".attempt:$finalDescription.suggestionChosen:${if (!wasChosen) -1 else displayOrderList.indexOf(chosenSuggestion) + 1}" +
                    ".api.${apiOrderList.indexOf(chosenSuggestion) + 1}.modified:$wasModified"
        )
    }

    fun logSuccess(context: Context, finalDescription: String, wasChosen: Boolean, wasModified: Boolean, title: PageTitle, revId: Long) {
        log(context, composeLogString(title) + ".success:$finalDescription.suggestionChosen:${if (!wasChosen) -1 else displayOrderList.indexOf(
            chosenSuggestion) + 1}.api.${apiOrderList.indexOf(chosenSuggestion) + 1}.modified:$wasModified.revId:$revId")
    }

    fun logSuggestionsReceived(context: Context, isBlp: Boolean, title: PageTitle) {
        apiFailed = false
        log(context, composeLogString(title) + ".blp:$isBlp.count:${apiOrderList.size}.api1:${apiOrderList.first()}" +
                 if (apiOrderList.size > 1) ".api2.${apiOrderList.last()}" else "")
    }

    fun logSuggestionsShown(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".count:${displayOrderList.size}.display1:${displayOrderList.first()} " +
                 if (displayOrderList.size > 1) ".display2.${displayOrderList.last()}" else "")
    }

    fun logSuggestionChosen(context: Context, suggestion: String, title: PageTitle) {
        chosenSuggestion = suggestion
        log(context, composeLogString(title) + ".selected:$suggestion.${getOrderString()}")
    }

    fun logSuggestionsDismissed(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".suggestionsDialog.dismissed")
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>, title: PageTitle) {
        val reportReasons = reportReasonsList.joinToString("|")
        log(context, composeLogString(title) + ".reportDialog.$suggestion.${getOrderString()}.reasons:$reportReasons.reported")
    }

    fun logReportDialogDismissed(context: Context) {
        log(context, composeGroupString() + ".reportDialog.dismissed")
    }

    fun logOnboardingShown(context: Context) {
        log(context, "$MACHINE_GEN_DESC_SUGGESTIONS.onboardingShown")
    }

    fun logGroupAssigned(context: Context, testGroup: Int) {
        log(context, "$MACHINE_GEN_DESC_SUGGESTIONS.groupAssigned:$testGroup")
    }

    fun logApiFailed(context: Context, throwable: Throwable, title: PageTitle) {
        if (throwable is HttpStatusException) {
            log(context, "Api failed with response code ${throwable.code} for : ${composeLogString(title)} ")
        }
        apiFailed = true
    }

    private fun log(context: Context, logString: String) {
        if (!isUserInExperiment || apiFailed) {
            return
        }
        EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), logString))
    }

    private fun getOrderString(): String {
        return "api.${apiOrderList.indexOf(chosenSuggestion) + 1}.display.${displayOrderList.indexOf(chosenSuggestion) + 1}"
    }

    private fun composeLogString(title: PageTitle): String {
        return "${composeGroupString()}.lang:${title.wikiSite.languageCode}.title:${title.prefixedText}"
    }

    private fun composeGroupString(): String {
        return "$MACHINE_GEN_DESC_SUGGESTIONS.group:${machineGeneratedDescriptionsABTest.aBTestGroup}.experienced:$isUserExperienced"
    }

    suspend fun setUserExperienced() =
        withContext(Dispatchers.Default) {
            val totalContributions = ServiceFactory.get(WikipediaApp.instance.wikiSite)
                .globalUserInfo(AccountUtil.userName!!).query?.globalUserInfo?.editCount ?: 0
            isUserExperienced = totalContributions > 50
        }
}
