package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp

class PatrollerExperienceEvent {

    companion object {
        private const val STREAM_NAME = "app_patroller_experience"

        fun logImpression(
            activeInterface: String,
            wikiId: String = ""
        ) {
            submitPatrollerActivityEvent(
                "impression",
                activeInterface,
                wikiId = wikiId
            )
        }

        fun logAction(
            action: String,
            activeInterface: String,
            actionData: String = "",
            wikiId: String = "",
        ) {
            submitPatrollerActivityEvent(
                action,
                activeInterface,
                actionData,
                wikiId
            )
        }

        fun getActionDataString(
            revisionId: Long? = null,
            feedbackOption: String? = null,
            feedbackText: String? = null,
            messageType: String? = null
        ): String {
            val revisionIdStr = revisionId?.let { "revision_id: $it, " }.orEmpty()
            val feedbackStr = feedbackOption?.let { "feedback: $it, " }.orEmpty()
            val feedbackTextStr = feedbackText?.let { "feedback_text: $it, " }.orEmpty()
            val savedMessageStr = messageType?.let { "saved_message: $it, " }.orEmpty()
            return revisionIdStr + feedbackStr + feedbackTextStr + savedMessageStr
        }

        private fun submitPatrollerActivityEvent(
            action: String,
            activeInterface: String,
            actionData: String = "",
            wikiId: String
        ) {
            AppInteractionEvent.STREAM_NAME = STREAM_NAME
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action,
                    activeInterface,
                    actionData,
                    WikipediaApp.instance.languageState.appLanguageCode,
                    wikiId,
                    "android"
                )
            )
        }
    }
}
