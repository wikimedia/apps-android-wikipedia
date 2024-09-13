package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.util.ActiveTimer
import org.wikipedia.util.UriUtil

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_image_recommendation_event/1.1.0")
class ImageRecommendationsEvent(
   private val action: String,
   private val active_interface: String,
   private val action_data: String,
   private val primary_language: String,
   private val wiki_id: String
) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.image_recommendation_event"
        val reasons = listOf("notrelevant", "noinfo", "offensive", "lowquality", "unfamiliar", "other")
        private val timer = ActiveTimer()

        fun logImpression(activeInterface: String, actionData: String = "", wikiId: String = "") {
            if (activeInterface == "recommendedimagetoolbar") {
                timer.reset()
            }
            submitImageRecommendationEvent("impression", activeInterface, actionData, wikiId)
        }

        fun logAction(action: String, activeInterface: String, actionData: String = "", wikiId: String = "") {
            submitImageRecommendationEvent(action, activeInterface, actionData, wikiId)
        }

        fun logEditSuccess(action: DescriptionEditActivity.Action, wikiId: String, revisionId: Long) {
            when (action) {
                DescriptionEditActivity.Action.ADD_DESCRIPTION -> logAction("edit_success", "se_add_description", getActionDataString(revisionId = revisionId), wikiId)
                DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> logAction("edit_success", "se_translate_description", getActionDataString(revisionId = revisionId), wikiId)
                DescriptionEditActivity.Action.ADD_CAPTION -> logAction("edit_success", "se_add_caption", getActionDataString(revisionId = revisionId), wikiId)
                DescriptionEditActivity.Action.TRANSLATE_CAPTION -> logAction("edit_success", "se_translate_caption", getActionDataString(revisionId = revisionId), wikiId)
                else -> logAction("edit_success", "se_add_image_tags", getActionDataString(revisionId = revisionId), wikiId)
            }
        }

        fun getActionDataString(
            filename: String? = null,
            recommendationSource: String? = null,
            recommendationSourceProjects: String? = null,
            rejectionReasons: String? = null,
            acceptanceState: String? = null,
            revisionId: Long? = null,
            captionAdd: Boolean? = null,
            altTextAdd: Boolean? = null,
            addTimeSpent: Boolean = false
        ): String {
            val filenameStr = filename?.let { "filename: ${UriUtil.encodeURL(filename)}, " }.orEmpty()
            val recSourceStr = recommendationSource?.let { "recommendation_source: $it, " }.orEmpty()
            val recSourceProjectsStr = recommendationSourceProjects?.let { "recommendation_source_project: $it, " }.orEmpty()
            val rejectionReasonsStr = rejectionReasons?.let { "rejection_reasons: $it, " }.orEmpty()
            val acceptanceStateStr = acceptanceState?.let { "acceptance_state: $it, " }.orEmpty()
            val revisionIdStr = revisionId?.let { "revision_id: $it, " }.orEmpty()
            val captionAddStr = captionAdd?.let { "caption_add: $it, " }.orEmpty()
            val altTextAddStr = altTextAdd?.let { "alt_text_add: $it, " }.orEmpty()
            val timeSpentStr = if (addTimeSpent) "time_spent: ${timer.elapsedMillis}" else ""
            return filenameStr + recSourceStr + recSourceProjectsStr + rejectionReasonsStr + acceptanceStateStr +
                    revisionIdStr + captionAddStr + altTextAddStr + timeSpentStr
        }

        private fun submitImageRecommendationEvent(action: String, activeInterface: String, actionData: String, wikiId: String) {
            EventPlatformClient.submit(ImageRecommendationsEvent(action, activeInterface, actionData, WikipediaApp.instance.languageState.appLanguageCode, wikiId))
        }
    }
}
