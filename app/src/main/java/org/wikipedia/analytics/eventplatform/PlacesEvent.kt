package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class PlacesEvent {
    companion object {
        private const val STREAM_NAME = "app_places_interaction"

        fun logImpression(activeInterface: String) {
            submitPlacesInteractionEvent("impression", activeInterface)
        }

        fun logAction(action: String, activeInterface: String) {
            submitPlacesInteractionEvent(action, activeInterface)
        }

        private fun submitPlacesInteractionEvent(action: String, activeInterface: String) {
            AppInteractionEvent.STREAM_NAME = STREAM_NAME
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action,
                    activeInterface,
                    "",
                    WikipediaApp.instance.languageState.appLanguageCode,
                    Prefs.placesWikiCode,
                    "android"
                )
            )
        }
    }
}
