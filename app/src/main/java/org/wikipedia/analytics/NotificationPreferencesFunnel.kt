package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done() {
        // TODO: remove?
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNotificationPreferences"
        private const val REV_ID = 18325724
    }
}
