package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class PatrollerExperienceEvent {

    companion object {
        fun logImpression(activeInterface: String) {
            submitPatrollerActivityEvent("impression", activeInterface)
        }

        fun logAction(action: String, activeInterface: String, actionData: String = "") {
            submitPatrollerActivityEvent(action, activeInterface, actionData)
        }

        fun getActionDataString(
            revisionId: Long? = null,
            feedbackOption: String? = null,
            feedbackText: String? = null,
            messageType: String? = null,
            summaryText: String? = null,
            filterSelected: String? = null,
            filterWiki: String? = null,
            filtersList: String? = null,
            appLanguageCodeAdded: String? = null,
            appLanguageCodes: String? = null,
        ): String {
            val revisionIdStr = revisionId?.let { "revision_id: $it, " }.orEmpty()
            val feedbackStr = feedbackOption?.let { "feedback: $it, " }.orEmpty()
            val feedbackTextStr = feedbackText?.let { "feedback_text: $it, " }.orEmpty()
            val savedMessageStr = messageType?.let { "message_type: $it, " }.orEmpty()
            val summaryTextStr = summaryText?.let { "summary_text: $it, " }.orEmpty()
            val wasSummaryAddedStr = summaryText?.let { summaryText.isNotEmpty().toString() }.orEmpty()
            val filterSelectedStr = filterSelected?.let { "filter_selected: $it, " }.orEmpty()
            val filterWikiStr = filterWiki?.let { "filter_wiki_selected: $it, " }.orEmpty()
            val filtersListStr = filtersList?.let { "filters_list: $it, " }.orEmpty()
            val appLanguageCodeAddedStr = appLanguageCodeAdded?.let { "app_language_code_added: $it, " }.orEmpty()
            val appLanguageCodesStr = appLanguageCodes?.let { "app_language_codes: $it, " }.orEmpty()
            return revisionIdStr + feedbackStr + feedbackTextStr + savedMessageStr + summaryTextStr + wasSummaryAddedStr +
                    filterSelectedStr + filterWikiStr + filtersListStr + appLanguageCodeAddedStr + appLanguageCodesStr
        }

        fun getPublishMessageActionString(isModified: Boolean? = null, isSaved: Boolean? = null, exampleMessage: String? = null): String {
            val isModifiedStr = isModified?.let { "is_modified: $it, " }.orEmpty()
            val isSavedStr = isSaved?.let { "is_saved: $it, " }.orEmpty()
            val exampleMessageStr = exampleMessage?.let { "example_message: $it, " }.orEmpty()
            return isModifiedStr + isSavedStr + exampleMessageStr
        }


        private fun submitPatrollerActivityEvent(action: String, activeInterface: String, actionData: String = "") {
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action,
                    activeInterface,
                    actionData,
                    WikipediaApp.instance.languageState.appLanguageCode,
                    Prefs.recentEditsWikiCode,
                    "app_patroller_experience"
                )
            )
        }
    }
}
