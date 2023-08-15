package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
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
        private const val ACTION_IMPRESSION = "impression"
        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        val reasons = listOf("notrelevant", "noinfo", "offensive", "lowquality", "unfamiliar", "other")

        fun logImpression(activeInterface: String, actionData: String = "", wikiId: String = "") {
            submitImageRecommendationEvent(ACTION_IMPRESSION, activeInterface, actionData, wikiId)
        }

        fun logAction(action: String, activeInterface: String, actionData: String, wikiId: String) {
            submitImageRecommendationEvent(action, activeInterface, actionData, wikiId)
        }

        fun getActionDataString(filename: String = "", recommendationSource: String = "",
                                rejectionReasons: String = "", acceptanceState: String = "", revisionId: String = "", captionAdd: String = "", altTextAdd: String = ""): String {
            return "filename:${URLEncoder.encode(filename, "UTF-8")}, recommendation_source:$recommendationSource, rejection_reasons:$rejectionReasons, " +
                    "acceptance_state:$acceptanceState revision_id:$revisionId, caption_add: $captionAdd, alt_text_add: $altTextAdd"
        }

        private fun submitImageRecommendationEvent(action: String, activeInterface: String, actionData: String, wikiId: String) {
            L.d("%%% action:$action, activeInterface:$activeInterface, actionData:$actionData,primary_language: ${WikipediaApp.instance.languageState.appLanguageCode}, wikiId: $wikiId")
            EventPlatformClient.submit(ImageRecommendationsEvent(action, activeInterface, actionData, WikipediaApp.instance.languageState.appLanguageCode, wikiId))
        }
    }
}
