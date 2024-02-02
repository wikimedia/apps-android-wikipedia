package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class PlacesEvent {
    companion object {

        fun logImpression(activeInterface: String) {
            submitPlacesInteractionEvent("impression", activeInterface)
        }

        fun logAction(action: String, activeInterface: String, actionData: String = "") {
            submitPlacesInteractionEvent(action, activeInterface, actionData)
        }

        private fun submitPlacesInteractionEvent(action: String, activeInterface: String, actionData: String = "") {
            EventPlatformClient.submit(
                AppInteractionEvent(
                    action,
                    activeInterface,
                    actionData,
                    WikipediaApp.instance.languageState.appLanguageCode,
                    Prefs.placesWikiCode,
                    "app_places_interaction"
                )
            )
        }
    }
}
