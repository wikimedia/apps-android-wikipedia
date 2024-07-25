package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.page.PageTitle

class MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private var apiFailed = false

    fun articleDescriptionEditingStart(context: Context) {
        log(context, composeGroupString() + ".start")
    }

    fun articleDescriptionEditingEnd(context: Context) {
        log(context, composeGroupString() + ".end")
    }

    fun logAttempt(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".attempt")
    }

    fun logSuccess(context: Context, title: PageTitle, revId: Long) {
        log(context, composeLogString(title) + ".success.revId:$revId")
    }

    fun logSuggestionsReceived(context: Context, isBlp: Boolean, title: PageTitle) {
        apiFailed = false
        log(context, composeLogString(title) + ".blp:$isBlp")
    }

    fun logSuggestionChosen(context: Context, suggestion: String, title: PageTitle) {
        log(context, composeLogString(title) + ".selected")
    }

    fun logSuggestionsDismissed(context: Context, title: PageTitle) {
        log(context, composeLogString(title) + ".suggestionsDialog.dismissed")
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>, title: PageTitle) {
        val reportReasons = reportReasonsList.joinToString("|")
        log(context, composeLogString(title) + ".reportDialog.suggestion:${encode(suggestion)}.reasons:$reportReasons.reported")
    }

    fun logReportDialogDismissed(context: Context) {
        log(context, composeGroupString() + ".reportDialog.dismissed")
    }

    fun logOnboardingShown(context: Context) {
        log(context, composeGroupString() + ".onboardingShown")
    }

    fun logApiFailed(context: Context, throwable: Throwable, title: PageTitle) {
        log(context, composeLogString(title) + ".apiError:${throwable.message}")
        apiFailed = true
    }

    private fun log(context: Context, logString: String) {
        if (apiFailed) {
            return
        }
        EventPlatformClient.submit(BreadCrumbLogEvent(BreadCrumbViewUtil.getReadableScreenName(context), logString))
    }

    private fun composeLogString(title: PageTitle): String {
        return "${composeGroupString()}.lang:${title.wikiSite.languageCode}.title:${encode(title.prefixedText)}"
    }

    private fun composeGroupString(): String {
        return "machineSuggestions"
    }

    companion object {
        // HACK: We're using periods and colons as delimiting characters in these events, so let's
        // urlencode just those characters, if they appear in our strings.
        private fun encode(str: String): String {
            return str.replace(".", "%2E").replace(":", "%3A")
        }
    }
}
