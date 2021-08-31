package org.wikipedia.analytics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.GsonUtil
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.settings.Prefs

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    @RequiresApi(Build.VERSION_CODES.O)
    fun done() {
        val toggleMap = HashMap<String, Boolean>()
        val notificationManagerCompat = NotificationManagerCompat.from(app)
        for (i in 0 until NotificationCategory.MAP.size()) {
            val channelId = NotificationCategory.MAP[i].id
            val importance = notificationManagerCompat.getNotificationChannel(channelId)?.importance
            // TODO: figure out the "Show notifications" status
            toggleMap[channelId] = importance != NotificationManagerCompat.IMPORTANCE_NONE
                    && importance != null && notificationManagerCompat.areNotificationsEnabled()
        }

        log(
            "type_toggles", GsonUtil.getDefaultGson().toJson(toggleMap),
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
