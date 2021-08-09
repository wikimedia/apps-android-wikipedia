package org.wikipedia.analytics

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.MoshiUtil
import org.wikipedia.notifications.Notification
import org.wikipedia.settings.Prefs

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done() {
        val toggleMap = mapOf(
            Notification.CATEGORY_SYSTEM_NO_EMAIL to Prefs.notificationWelcomeEnabled(),
            Notification.CATEGORY_EDIT_THANK to Prefs.notificationThanksEnabled(),
            Notification.CATEGORY_MILESTONE_EDIT to Prefs.notificationMilestoneEnabled()
        )
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Boolean::class.java)
        val adapter: JsonAdapter<Map<String, Boolean>> = MoshiUtil.getDefaultMoshi().adapter(type)
        log(
                "type_toggles", adapter.toJson(toggleMap),
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
