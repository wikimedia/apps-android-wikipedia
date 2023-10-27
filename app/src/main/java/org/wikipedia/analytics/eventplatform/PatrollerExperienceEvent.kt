package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/app_interaction/1.0.0")
class PatrollerExperienceEvent(
    private val action: String,
    private val active_interface: String,
    private val action_data: String,
    private val primary_language: String,
    private val wiki_id: String,
    private var platform: String
) : MobileAppsEvent(STREAM_NAME) {

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
            EventPlatformClient.submit(
                PatrollerExperienceEvent(
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
