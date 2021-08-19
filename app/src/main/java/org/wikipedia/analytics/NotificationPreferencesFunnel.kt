package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.settings.Prefs
import java.util.*

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done() {
        val toggleMap = HashMap<String, Boolean>()
        toggleMap[NotificationCategory.SYSTEM_NO_EMAIL.id] = Prefs.notificationWelcomeEnabled()
        toggleMap[NotificationCategory.EDIT_THANK.id] = Prefs.notificationThanksEnabled()
        toggleMap[NotificationCategory.MILESTONE_EDIT.id] = Prefs.notificationMilestoneEnabled()
        log(
                "type_toggles", GsonMarshaller.marshal(toggleMap),
                "background_fetch",
                if (Prefs.notificationPollEnabled()) app.resources.getInteger(R.integer.notification_poll_interval_minutes).toString()
                else "disabled"
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNotificationPreferences"
        private const val REV_ID = 18325724
    }
}
