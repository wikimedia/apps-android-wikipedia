package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.WikipediaApp

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private const val MACHINE_GEN_DESC_SUGGESTIONS = "MachineGeneratedArticleSuggestions"
    private val testGroup: Int
        get() { return if (WikipediaApp.instance.machineGeneratedDescriptionsABTest.isEnrolled) WikipediaApp.instance.machineGeneratedDescriptionsABTest.aBTestGroup else -1 }

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

    fun logActualPublishedDescription(context: Context, publishedDescription: String, wikidataEdit: Boolean = false) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.publishedTo.${if (wikidataEdit) "wikidata" else "articleOnWiki"}publishedDescription.$publishedDescription.published"
            )
        )
    }

    fun suggestedDescriptionsButtonShown(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$testGroup.suggestedDescriptionsButton.shown"
            )
        )
    }

    fun machineGeneratedSuggestionsDetailsLogged(context: Context, articleName: String,
                                                 suggestionsList: List<String>, isBlp: Boolean) {
        val suggestions = suggestionsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$$testGroup.ApiResponseDetails.articleName:$articleName.isBlp:$isBlp" +
                        ".NumberOfSuggestionsOffered:${suggestionsList.size}.Suggestions:$suggestions.logged"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogSuggestionChosen(context: Context, suggestion: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$testGroup.suggestionsDialogs.chosenSuggestion:$suggestion"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogOptedOut(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$testGroup.suggestionsDialogs.optedOut"
            )
        )
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$testGroup.ReportDialog.$suggestion.reportReasons:$reportReasons.reported"
            )
        )
    }

    fun logReportDialogOptedOut(context: Context, suggestion: String, reportReasonsList: List<String>) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserInGroup.$testGroup" +
                        ".ReportDialog.$suggestion.reportReasons:$reportReasons.optedOut"
            )
        )
    }

    fun logAiOnBoardingCardShown(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserAssignedTo.Group.$testGroup.AiOnBoardingCard.shown"
            )
        )
    }

    fun logUserGroupAssigned(context: Context, testGroup: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserAssignedTo.Group.$testGroup"
            )
        )
    }
}
