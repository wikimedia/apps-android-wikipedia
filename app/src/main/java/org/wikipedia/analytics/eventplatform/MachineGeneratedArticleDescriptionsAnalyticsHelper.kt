package org.wikipedia.analytics.eventplatform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory

object MachineGeneratedArticleDescriptionsAnalyticsHelper {

    private const val MACHINE_GEN_DESC_SUGGESTIONS = "MachineGeneratedArticleSuggestions"
    val machineGeneratedDescriptionsABTest = MachineGeneratedArticleDescriptionABCTest()

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

    fun logActualPublishedDescription(context: Context, finalDescription: String, articleWiki: String, articleName: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.articleWiki.$articleWiki.articleName.$articleName.finalDescription.$finalDescription.published"
            )
        )
    }

    fun suggestedDescriptionsButtonShown(context: Context, articleWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.articleWiki.$articleWiki.suggestedDescriptionsButton.shown"
            )
        )
    }

    fun machineGeneratedSuggestionsDetailsLogged(context: Context, suggestionsList: List<String>,
                                                 isBlp: Boolean, articleWiki: String, articleName: String) {
        val suggestions = suggestionsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.ApiResponseDetails.articleWiki.$articleWiki.title:$articleName.blp:$isBlp" +
                        ".count:${suggestionsList.size}.Suggestions:$suggestions.logged"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogSuggestionChosen(context: Context, suggestion: String, articleWiki: String, articleName: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.suggestionsDialogs.chosen:$suggestion.articleWiki.$articleWiki.title:$articleName"
            )
        )
    }

    fun machineGeneratedSuggestionsDialogOptedOut(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.suggestionsDialogs.dismissed"
            )
        )
    }

    fun logSuggestionReported(context: Context, suggestion: String, reportReasonsList: List<String>, articleWiki: String, articleName: String) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.ReportDialog.reportedSuggestion.$suggestion.articleWiki" +
                        ".$articleWiki.articleName:$articleName.reasons:$reportReasons.reported"
            )
        )
    }

    fun logReportDialogOptedOut(context: Context, suggestion: String, reportReasonsList: List<String>) {
        val reportReasons = reportReasonsList.joinToString(",")
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}" +
                        ".ReportDialog.$suggestion.reasons:$reportReasons.optedOut"
            )
        )
    }

    fun logAiOnBoardingCardShown(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "$MACHINE_GEN_DESC_SUGGESTIONS.UserGroup.${machineGeneratedDescriptionsABTest.aBTestGroup}.AiOnBoardingCard.shown"
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

    suspend fun isUserExperienced(): Boolean =
        withContext(Dispatchers.Default) {
            val totalContributions = ServiceFactory.get(WikipediaApp.instance.wikiSite)
                .globalUserInfo(AccountUtil.userName!!).query?.globalUserInfo?.editCount ?: 0

            return@withContext totalContributions > 50
        }
}
