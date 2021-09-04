package org.wikipedia.analytics

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.squareup.moshi.Types
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.MoshiUtil
import org.wikipedia.notifications.NotificationCategory

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val toggleMap = HashMap<String, Boolean>()
            val notificationManagerCompat = NotificationManagerCompat.from(app)
            for (i in 0 until NotificationCategory.MAP.size()) {
                val channelId = NotificationCategory.MAP[i].id
                val importance =
                    notificationManagerCompat.getNotificationChannel(channelId)?.importance
                // TODO: figure out the "Show notifications" status
                toggleMap[channelId] = importance != NotificationManagerCompat.IMPORTANCE_NONE &&
                        importance != null && notificationManagerCompat.areNotificationsEnabled()
            }

            val type = Types.newParameterizedType(Map::class.java, String::class.java, Boolean::class.javaObjectType)
            val adapter = MoshiUtil.getDefaultMoshi().adapter<Map<String, Boolean>>(type)
            log(
                "type_toggles", adapter.toJson(toggleMap),
                "background_fetch", app.resources.getInteger(R.integer.notification_poll_interval_minutes)
            )
        }
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNotificationPreferences"
        private const val REV_ID = 18325724
    }
}
