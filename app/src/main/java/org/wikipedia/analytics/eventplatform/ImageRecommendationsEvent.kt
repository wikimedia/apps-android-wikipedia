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

        fun logImpression(activeInterface: String, actionData: String, wikiId: String) {
            submitImageRecommendationEvent(ACTION_IMPRESSION, activeInterface, actionData, wikiId)
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
