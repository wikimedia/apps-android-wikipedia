package org.wikipedia.analytics.eventplatform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private const val MACHINE_GEN_DESC_SUGGESTIONS = "machineSuggestions"
    val machineGeneratedDescriptionsABTest = MachineGeneratedArticleDescriptionABCTest()

    var isUserExperienced = false
    var isUserInExperiment = false

    fun articleDescriptionEditingStart(context: Context) {
        log(context, "ArticleDescriptionEditing.start")
    }

    fun articleDescriptionEditingEnd(context: Context) {
        log(context, "ArticleDescriptionEditing.end")
    }

    fun logAttempt(context: Context, finalDescription: String, wasSuggestionModified: Boolean, title: PageTitle) {
        log(context, composeLogString(title) + ".attempt:$finalDescription.modified:$wasSuggestionModified")
    }

    fun logSuccess(context: Context, finalDescription: String, wasChosen: Boolean, wasModified: Boolean, title: PageTitle, revId: Long) {
        log(context, composeLogString(title) + ".success:$finalDescription.suggestionChosen:$wasChosen.modified:$wasModified.revId:$revId")
    }

    fun logSuggestionsReceived(context: Context, suggestionsList: List<String>, isBlp: Boolean, title: PageTitle) {
        val suggestions = suggestionsList.joinToString("|")
        log(context, composeLogString(title) + ".blp:$isBlp.count:${suggestionsList.size}.suggestions:$suggestions")
    }

    fun logSuggestionsShown(context: Context, suggestionsList: List<String>, title: PageTitle) {
        val suggestions = suggestionsList.joinToString("|")
        log(context, composeLogString(title) + ".count:${suggestionsList.size}.displayOrder:$suggestions")
    }

    fun logSuggestionSelected(context: Context, suggestion: String, title: PageTitle) {
        log(context, composeLogString(title) + ".selected:$suggestion")
    }

    fun logSuggestionsDismissed(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".suggestionsDialog.dismissed")
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>, title: PageTitle) {
        val reportReasons = reportReasonsList.joinToString("|")
        log(context, composeLogString(title) + ".reportDialog.$suggestion.reasons:$reportReasons.reported")
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

    private fun log(context: Context, logString: String) {
        if (!isUserInExperiment) {
            return
        }
        EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), logString))
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
