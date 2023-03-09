package org.wikipedia.analytics.eventplatform

import android.content.Context

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    fun articleDescriptionEditingStart(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ArticleDescriptionEditing.start"
            )
        )
    }

    fun articleDescriptionEditingEnd(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ArticleDescriptionEditing.end"
            )
        )
    }

    fun suggestedDescriptionsButtonShown(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestion.suggestedDescriptionsButton.shown"
            )
        )
    }

    fun machineGeneratedSuggestionsDetailsLogged(context: Context, articleName: String,
                                                 suggestionsList: List<String>, isBlp: Boolean) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestionsApiResponseDetails.articleName:$articleName.isBlp:$isBlp.NumberOfSuggestionsOffered:${suggestionsList.size}.Suggestions:${
                    suggestionsList.joinToString(",")
                }.logged"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogSuggestionChosen(context: Context, suggestion: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestion.suggestionsDialogs.chosenSuggestion:$suggestion"
            )
        )
    }
    fun machineGeneratedSuggestionsDialogDismissed(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestion.suggestionsDialogs.dismissed"
            )
        )
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestions.ReportDialog.$suggestion.reportReasons:${
                    reportReasonsList.joinToString(",")
                }.reported"
            )
        )
    }

    fun logReportDialogCancelled(context: Context, suggestion: String, reportReasonsList: List<String>) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "MachineGeneratedArticleSuggestions.ReportDialog.$suggestion.reportReasons:${
                    reportReasonsList.joinToString(",")
                }.cancelled"
            )
        )
    }
}
