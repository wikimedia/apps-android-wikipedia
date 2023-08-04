package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp

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

        fun logImpression(activeInterface: String) {
            submitImageRecommendationEvent(ACTION_IMPRESSION, activeInterface, "", "")
        }

        fun logAction(action: String, activeInterface: String, actionData: String, wikiId: String) {
            submitImageRecommendationEvent(action, activeInterface, actionData, wikiId)
        }

        fun getActionDataString(filename: String = "", recommendationSource: String = "", recommendationSourceProject: String = "",
                                rejectionReasons: String = "", acceptanceState: String = "", seriesNumber: String = "",
                                totalSuggestions: String = "", revisionId: String = "", captionAdd: String = "", altTextAdd: String = ""): String {
            return "filename:$filename, recommendation_source:$recommendationSource,recommendation_source_project:$recommendationSourceProject, " +
                    "rejection_reasons:$rejectionReasons, acceptance_state:$acceptanceState, series_number: $seriesNumber," +
                    "total_suggestions: $totalSuggestions, revision_id:$revisionId, caption_add: $captionAdd, alt_text_add: $altTextAdd"
        }

        private fun submitImageRecommendationEvent(action: String, activeInterface: String, actionData: String, wikiId: String) {
            EventPlatformClient.submit(ImageRecommendationsEvent(action, activeInterface, actionData, WikipediaApp.instance.languageState.appLanguageCode, wikiId))
        }
    }
}
