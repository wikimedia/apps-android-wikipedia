package org.wikipedia.analytics.eventplatform

import android.content.Context

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private const val MACHINE_GEN_DESC_SUGGESTIONS = "MachineGeneratedArticleSuggestions"

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
                "$MACHINE_GEN_DESC_SUGGESTIONS.suggestedDescriptionsButton.shown"
            )
        )
    }

    fun machineGeneratedSuggestionsDetailsLogged(context: Context, articleName: String,
                                                 suggestionsList: List<String>, isBlp: Boolean) {
        val suggestions = suggestionsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.ApiResponseDetails.articleName:$articleName.isBlp:$isBlp" +
                        ".NumberOfSuggestionsOffered:${suggestionsList.size}.Suggestions:$suggestions.logged"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogSuggestionChosen(context: Context, suggestion: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.suggestionsDialogs.chosenSuggestion:$suggestion"
            )
        )
    }
    fun machineGeneratedSuggestionsDialogOptedOut(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.suggestionsDialogs.optedOut"
            )
        )
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.ReportDialog.$suggestion.reportReasons:$reportReasons.reported"
            )
        )
    }

    fun logReportDialogOptedOut(context: Context, suggestion: String, reportReasonsList: List<String>) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.ReportDialog.$suggestion.reportReasons:$reportReasons.optedOut"
            )
        )
    }
}
