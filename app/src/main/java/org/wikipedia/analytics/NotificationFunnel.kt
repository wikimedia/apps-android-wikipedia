package org.wikipedia.analytics

import android.content.Intent
import org.json.JSONObject
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.NotificationPollBroadcastReceiver

class NotificationFunnel(app: WikipediaApp, private val id: Long, private val wiki: String, private val type: String?) : Funnel(app, SCHEMA_NAME, REV_ID) {

    constructor(app: WikipediaApp, notification: Notification) : this(app, notification.id(), notification.wiki(), notification.type())

    override fun preprocessData(eventData: JSONObject): JSONObject {
        preprocessData(eventData, "notification_id", id)
        preprocessData(eventData, "notification_wiki", wiki)
        preprocessData(eventData, "notification_type", type)
        return super.preprocessData(eventData)
    }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun logMarkRead(selectionToken: Long?) {
        log("action_rank", 0, "selection_token", selectionToken)
    }

    fun logAction(index: Int, link: Notification.Link) {
        log("action_rank", index + 1, "action_icon", link.icon)
    }

    fun logClicked() {
        log("action_rank", ACTION_CLICKED)
    }

    fun logDismissed() {
        log("action_rank", ACTION_DISMISSED)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNotificationInteraction"
        private const val REV_ID = 18325732
        private const val ACTION_CLICKED = 10
        private const val ACTION_DISMISSED = 11

        fun processIntent(intent: Intent) {
            if (!intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID)) {
                return
            }
            val funnel = NotificationFunnel(WikipediaApp.getInstance(),
                    intent.getLongExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, 0),
                    WikipediaApp.getInstance().wikiSite.dbName(),
                    intent.getStringExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE))
            if (NotificationPollBroadcastReceiver.ACTION_CANCEL == intent.action) {
                funnel.logDismissed()
            } else {
                funnel.logClicked()
            }
        }
    }
}
