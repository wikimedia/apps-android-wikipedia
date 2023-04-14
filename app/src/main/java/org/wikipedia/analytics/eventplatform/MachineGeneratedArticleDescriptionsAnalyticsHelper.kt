package org.wikipedia.analytics.eventplatform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ActiveTimer

class MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private var apiFailed = false
    var apiOrderList = emptyList<String>()
    var displayOrderList = emptyList<String>()
    private var chosenSuggestion = ""
    val timer = ActiveTimer()

    fun articleDescriptionEditingStart(context: Context) {
        log(context, composeGroupString() + ".start")
    }

    fun articleDescriptionEditingEnd(context: Context) {
        log(context, composeGroupString() + ".end.timeSpentMs:${timer.elapsedMillis}")
    }

    fun logAttempt(context: Context, finalDescription: String, wasChosen: Boolean, wasModified: Boolean, title: PageTitle) {
        log(context, composeLogString(title) + ".attempt:$finalDescription${getSuggestionOrderString(wasChosen, wasModified)}" +
                ".timeSpentMs:${timer.elapsedMillis}")
    }

    fun logSuccess(context: Context, finalDescription: String, wasChosen: Boolean, wasModified: Boolean, title: PageTitle, revId: Long) {
        log(context, composeLogString(title) + ".success:$finalDescription${getSuggestionOrderString(wasChosen, wasModified)}" +
                    ".timeSpentMs:${timer.elapsedMillis}.revId:$revId")
    }

    private fun getSuggestionOrderString(wasChosen: Boolean, wasModified: Boolean): String {
       return if (apiOrderList.isEmpty() || displayOrderList.isEmpty()) {
           ""
       } else {
           ".suggestion1:${encode(apiOrderList.first())}" + (if (apiOrderList.size > 1) ".suggestion2:${encode(apiOrderList.last())}" else "") +
                   getOrderString(wasChosen, chosenSuggestion) + ".modified:$wasModified"
       }
    }

    fun logSuggestionsReceived(context: Context, isBlp: Boolean, title: PageTitle) {
        apiFailed = false
        log(context, composeLogString(title) + ".blp:$isBlp.count:${apiOrderList.size}.suggestion1:${encode(apiOrderList.first())}" +
                if (apiOrderList.size > 1) ".suggestion2:${encode(apiOrderList.last())}" else "")
    }

    fun logSuggestionsShown(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".count:${displayOrderList.size}.display1:${encode(displayOrderList.first())}" +
                if (displayOrderList.size > 1) ".display2:${encode(displayOrderList.last())}" else "")
    }

    fun logSuggestionChosen(context: Context, suggestion: String, title: PageTitle) {
        chosenSuggestion = suggestion
        log(context, composeLogString(title) + ".selected:${encode(suggestion)}${getOrderString(true, suggestion)}")
    }

    fun logSuggestionsDismissed(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".suggestionsDialog.dismissed")
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>, title: PageTitle) {
        val reportReasons = reportReasonsList.joinToString("|")
        log(context, composeLogString(title) + ".reportDialog.suggestion:${encode(suggestion)}${getOrderString(true, suggestion)}.reasons:$reportReasons.reported")
    }

    fun logReportDialogDismissed(context: Context) {
        log(context, composeGroupString() + ".reportDialog.dismissed")
    }

    fun logOnboardingShown(context: Context) {
        log(context, composeGroupString() + ".onboardingShown")
    }

    fun logGroupAssigned(context: Context, testGroup: Int) {
        log(context, "$MACHINE_GEN_DESC_SUGGESTIONS.groupAssigned:$testGroup")
    }

    fun logApiFailed(context: Context, throwable: Throwable, title: PageTitle) {
        log(context, composeLogString(title) + ".apiError:${throwable.message}")
        apiFailed = true
    }

    private fun log(context: Context, logString: String) {
        if (!isUserInExperiment || apiFailed) {
            return
        }
        EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), logString))
    }

    private fun getOrderString(wasChosen: Boolean, suggestion: String): String {
        return ".chosenApiIndex:${if (!wasChosen) -1 else apiOrderList.indexOf(suggestion) + 1}" +
                ".chosenDisplayIndex:${if (!wasChosen) -1 else displayOrderList.indexOf(suggestion) + 1}"
    }

    private fun composeLogString(title: PageTitle): String {
        return "${composeGroupString()}.lang:${title.wikiSite.languageCode}.title:${encode(title.prefixedText)}"
    }

    private fun composeGroupString(): String {
        if (!isUserInExperiment) {
            return ""
        }
        return "$MACHINE_GEN_DESC_SUGGESTIONS.group:${abcTest.group}.experienced:${Prefs.suggestedEditsMachineGeneratedDescriptionsIsExperienced}"
    }

    companion object {
        private const val MACHINE_GEN_DESC_SUGGESTIONS = "machineSuggestions"
        val abcTest = MachineGeneratedArticleDescriptionABCTest()
        var isUserInExperiment = false

        // HACK: We're using periods and colons as delimiting characters in these events, so let's
        // urlencode just those characters, if they appear in our strings.
        private fun encode(str: String): String {
            return str.replace(".", "%2E").replace(":", "%3A")
        }

        suspend fun setUserExperienced() =
            withContext(Dispatchers.Default) {
                val totalContributions = ServiceFactory.get(WikipediaApp.instance.wikiSite)
                    .globalUserInfo(AccountUtil.userName!!).query?.globalUserInfo?.editCount ?: 0
                Prefs.suggestedEditsMachineGeneratedDescriptionsIsExperienced = totalContributions > 50
            }
    }
}
