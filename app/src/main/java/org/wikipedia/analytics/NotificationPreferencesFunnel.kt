package org.wikipedia.analytics

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationsFilterActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

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

            log(
                "type_toggles", JsonUtil.encodeToString(toggleMap),
                "background_fetch", app.resources.getInteger(R.integer.notification_poll_interval_minutes)
            )
        }
    }

    fun logNotificationFilterPrefs() {
        val fullFiltersList = mutableListOf<String>()
        val toggleMap = HashMap<String, Boolean>()
        val filteredList = Prefs.notificationsFilterLanguageCodes.orEmpty()
        fullFiltersList.addAll(NotificationsFilterActivity.allWikisList())
        fullFiltersList.addAll(NotificationsFilterActivity.allTypesIdList())
        fullFiltersList.forEach { toggleMap[it] = filteredList.contains(it) }
        log("type_toggles", JsonUtil.encodeToString(toggleMap))
    }

    fun logSearchClick() {
        log("type_toggles", "search_clicked")
    }

    fun logFilterClick() {
        log("type_toggles", "filter_clicked")
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNotificationPreferences"
        private const val REV_ID = 22083261
    }
}
