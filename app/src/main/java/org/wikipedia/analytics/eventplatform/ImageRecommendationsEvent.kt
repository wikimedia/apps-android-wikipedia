package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp

class ImageRecommendationsEvent(private val event: ImageRecommendationsImplEvent) :
    MobileAppsEvent(STREAM_NAME) {

    companion object {

        const val ACTION_IMPRESSION = "impression"
        const val INTERFACE_OTHER = "other"

        private const val STREAM_NAME = "eventlogging_EditAttemptStep"
        private const val INTEGRATION_ID = "app-android"

        fun logImpression(activeInterface: String) {
            submitImageRecommendationEvent(ACTION_IMPRESSION, activeInterface, "", "")
        }

        fun logAction(action: String, activeInterface: String, actionData: String, wikiId: String) {
            submitImageRecommendationEvent(action, activeInterface, actionData, wikiId)
        }

        fun getActionDataString(filename: String = "", recommendationSource: String = "", recommendationSourceProject: String = "", rejectionReasons: String = "",
                                acceptanceState: String = "", seriesNumber: String = "", totalSuggestions: String = "", revisionId: String = ""): String {
            return "filename:$filename, recommendation_source:$recommendationSource,recommendation_source_project:$recommendationSourceProject, " +
                    "rejection_reasons:$rejectionReasons, acceptance_state:$acceptanceState, series_number: $seriesNumber," +
                    "total_suggestions: $totalSuggestions, revision_id:$revisionId"
        }

        private fun submitImageRecommendationEvent(action: String, activeInterface: String, actionData: String, wikiId: String) {
            EventPlatformClient.submit(ImageRecommendationsEvent(ImageRecommendationsImplEvent(action, activeInterface, actionData,
                WikipediaApp.instance.languageState.appLanguageCode, wikiId)))
        }
    }
}

@Suppress("unused")
@Serializable
class ImageRecommendationsImplEvent(
    private val action: String,
    private val active_interface: String,
    private val action_data: String,
    private val primary_language: String,
    private val wiki_id: String
)
