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

class NotificationPreferencesFunnel(app: WikipediaApp) : Funnel(app, SCHEMA_NAME, REV_ID) {

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun done() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat = NotificationManagerCompat.from(app)
            val toggleMap = NotificationCategory.MAP.valueIterator().asSequence().associate {
                val importance = notificationManagerCompat.getNotificationChannel(it.id)?.importance
                // TODO: figure out the "Show notifications" status
                it.id to (importance != NotificationManagerCompat.IMPORTANCE_NONE &&
                        importance != null && notificationManagerCompat.areNotificationsEnabled())
            }

            log(
                "type_toggles", JsonUtil.encodeToString(toggleMap),
                "background_fetch", app.resources.getInteger(R.integer.notification_poll_interval_minutes)
            )
        }
    }

    fun logNotificationFilterPrefs() {
        val fullFiltersList = mutableListOf<String>()
        val toggleMap = mutableMapOf<String, Boolean>()
        val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
        val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
        fullFiltersList.addAll(NotificationsFilterActivity.allWikisList())
        fullFiltersList.addAll(NotificationsFilterActivity.allTypesIdList())
        fullFiltersList.forEach { toggleMap[it] = !excludedWikiCodes.contains(it) && !excludedTypeCodes.contains(it) }
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
