package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.util.ActiveTimer
import org.wikipedia.util.log.L
import java.net.URLEncoder

@Serializable
@SerialName("/analytics/mobile_apps/android_image_recommendation_event/1.0.0")
class ImageRecommendationsEvent(
   private val action: String,
   private val active_interface: String,
   private val action_data: String,
   private val primary_language: String,
   private val wiki_id: String
) : MobileAppsEvent(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        val reasons = listOf("notrelevant", "noinfo", "offensive", "lowquality", "unfamiliar", "other")
        private val timer = ActiveTimer()

        fun logImpression(activeInterface: String, actionData: String = "", wikiId: String = "") {
            if (activeInterface == "recommendedimagetoolbar") {
                timer.reset()
            }
            submitImageRecommendationEvent("impression", activeInterface, actionData, wikiId)
        }

        fun logAction(action: String, activeInterface: String, actionData: String, wikiId: String) {
            if (action == "back" && activeInterface == "recommendedimagetoolbar") {
                //  Todo:  stop timer
            }
            submitImageRecommendationEvent(action, activeInterface, actionData, wikiId)
        }

        fun logSeEditSuccess(action: DescriptionEditActivity.Action, wikiId: String, l: Long) {
            when (action) {
                DescriptionEditActivity.Action.ADD_DESCRIPTION -> logAction("edit_success", "se_add_description", "", wikiId)
                DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION -> logAction("edit_success", "se_translate_description", "", wikiId)
                DescriptionEditActivity.Action.ADD_CAPTION -> logAction("edit_success", "se_add_caption", "", wikiId)
                DescriptionEditActivity.Action.TRANSLATE_CAPTION -> logAction("edit_success", "se_translate_caption", "", wikiId)
                else -> logAction("edit_success", "se_add_image_tags", "", wikiId)
            }
        }

        fun getActionDataString(filename: String = "", recommendationSource: String = "", rejectionReasons: String = "", acceptanceState: String = "",
                                revisionId: String = "", captionAdd: Boolean? = null, altTextAdd: Boolean? = null, addTimeSpent: Boolean = false): String {
            return "filename:${URLEncoder.encode(filename, "UTF-8")}, recommendation_source:$recommendationSource, rejection_reasons:$rejectionReasons, " +
                    "acceptance_state:$acceptanceState revision_id:$revisionId, caption_add: ${captionAdd ?: ""}, alt_text_add: ${altTextAdd ?: ""}, " +
                    "timeSpent:${if (addTimeSpent) timer.elapsedMillis.toString() else ""}"
        }

        private fun submitImageRecommendationEvent(action: String, activeInterface: String, actionData: String, wikiId: String) {
            L.d("%%% action:$action, activeInterface:$activeInterface, actionData:$actionData,primary_language: ${WikipediaApp.instance.languageState.appLanguageCode}, wikiId: $wikiId")
            EventPlatformClient.submit(ImageRecommendationsEvent(action, activeInterface, actionData, WikipediaApp.instance.languageState.appLanguageCode, wikiId))
        }
    }
}
