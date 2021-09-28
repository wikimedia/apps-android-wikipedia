package org.wikipedia.analytics.eventplatform

import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.NotificationPollBroadcastReceiver

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/android_notification_interaction/1.0.0")
class NotificationInteractionEvent(
    private val notification_id: Int,
    private val notification_wiki: String,
    private val notification_type: String,
    private val action_rank: Int,
    private val action_icon: String,
    private val selection_token: String,
    private val incoming_only: Boolean,
    private val device_level_enabled: Boolean
) : Event(STREAM_NAME) {

    companion object {
        private const val STREAM_NAME = "android.notification_interaction"

        private const val ACTION_INCOMING = -1
        private const val ACTION_READ_AND_ARCHIVED = 0
        private const val ACTION_CLICKED = 10
        private const val ACTION_DISMISSED = 11
        const val ACTION_PRIMARY = 1
        const val ACTION_SECONDARY = 2
        const val ACTION_LINK_CLICKED = 3

        private fun logClicked(intent: Intent) {
            EventPlatformClient.submit(NotificationInteractionEvent(intent.getLongExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, 0).toInt(),
                WikipediaApp.getInstance().wikiSite.dbName(), intent.getStringExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE).orEmpty(), ACTION_CLICKED,
                "", "", incoming_only = false, device_level_enabled = true))
        }

        private fun logDismissed(intent: Intent) {
            EventPlatformClient.submit(NotificationInteractionEvent(intent.getLongExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, 0).toInt(),
                WikipediaApp.getInstance().wikiSite.dbName(), intent.getStringExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE).orEmpty(), ACTION_DISMISSED,
                "", "", incoming_only = false, device_level_enabled = true))
        }

        fun logMarkRead(notification: Notification, selectionToken: Long?) {
            EventPlatformClient.submit(NotificationInteractionEvent(notification.id.toInt(), notification.wiki, notification.type, ACTION_READ_AND_ARCHIVED,
                "", selectionToken?.toString()
                    ?: "", incoming_only = false, device_level_enabled = true))
        }

        fun logIncoming(notification: Notification, type: String?) {
            EventPlatformClient.submit(NotificationInteractionEvent(notification.id.toInt(), notification.wiki, type ?: notification.type, ACTION_INCOMING,
                "", "", incoming_only = true, device_level_enabled = NotificationManagerCompat.from(WikipediaApp.getInstance()).areNotificationsEnabled()))
        }

        fun logAction(notification: Notification, index: Int, link: Notification.Link) {
            EventPlatformClient.submit(NotificationInteractionEvent(notification.id.toInt(), notification.wiki, notification.type, index,
                link.icon(), "", incoming_only = false, device_level_enabled = true))
        }

        fun processIntent(intent: Intent) {
            if (!intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID)) {
                return
            }
            if (NotificationPollBroadcastReceiver.ACTION_CANCEL == intent.action) {
                logDismissed(intent)
            } else {
                logClicked(intent)
            }
        }
    }
}
