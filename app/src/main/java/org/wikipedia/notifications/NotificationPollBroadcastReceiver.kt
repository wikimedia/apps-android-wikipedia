package org.wikipedia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.UnreadNotificationsEvent
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.db.Notification
import org.wikipedia.page.PageTitle
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.NotificationDirectReplyHelper
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.util.concurrent.TimeUnit

class NotificationPollBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when {
            Intent.ACTION_BOOT_COMPLETED == intent.action -> {
                // To test the BOOT_COMPLETED intent:
                // `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

                // Update our channel name, if needed.
                L.d("channel=" + ReleaseUtil.getChannel(context))
                startPollTask(context)
            }
            ACTION_POLL == intent.action -> {
                if (!AccountUtil.isLoggedIn) {
                    return
                }
                maybeShowLocalNotificationForEditorReactivation(context)

                // If push notifications are active, then don't actually do any polling.
                if (WikipediaFirebaseMessagingService.isUsingPush()) {
                    return
                }
                PollNotificationWorker.schedulePollNotificationJob(context)
            }
            ACTION_CANCEL == intent.action -> {
                NotificationInteractionEvent.processIntent(intent)
                org.wikipedia.analytics.metricsplatform.NotificationInteractionEvent.processIntent(intent)
            }
            ACTION_DIRECT_REPLY == intent.action -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val text = remoteInput?.getCharSequence(RESULT_KEY_DIRECT_REPLY)

                val wiki = intent.parcelableExtra<WikiSite>(Constants.ARG_WIKISITE)
                val title = intent.parcelableExtra<PageTitle>(Constants.ARG_TITLE)
                if (wiki != null && title != null && !text.isNullOrEmpty()) {
                    NotificationDirectReplyHelper.handleReply(context, wiki, title, text.toString(),
                        intent.getStringExtra(RESULT_EXTRA_REPLY_TO).orEmpty(),
                        intent.getIntExtra(RESULT_EXTRA_ID, 0))
                }
            }
        }
    }

    companion object {
        const val ACTION_POLL = "action_notification_poll"
        const val ACTION_CANCEL = "action_notification_cancel"
        const val ACTION_DIRECT_REPLY = "action_direct_reply"
        const val RESULT_KEY_DIRECT_REPLY = "key_direct_reply"
        const val RESULT_EXTRA_REPLY_TO = "extra_reply_to"
        const val RESULT_EXTRA_ID = "extra_id"
        const val TYPE_MULTIPLE = "multiple"

        private const val TYPE_LOCAL = "local"
        private const val FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 3
        private const val SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 7

        fun startPollTask(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(),
                        TimeUnit.MINUTES.toMillis((context.resources.getInteger(R.integer.notification_poll_interval_minutes) /
                                if (Prefs.isSuggestedEditsReactivationTestEnabled && !ReleaseUtil.isDevRelease) 10 else 1).toLong()),
                        getAlarmPendingIntent(context))
            } catch (e: Exception) {
                // There seems to be a Samsung-specific issue where it doesn't update the existing
                // alarm correctly and adds it as a new one, and eventually hits the limit of 500
                // concurrent alarms, causing a crash.
                L.e(e)
            }
        }

        fun stopPollTask(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(getAlarmPendingIntent(context))
        }

        private fun getAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
            intent.action = ACTION_POLL
            return PendingIntentCompat.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)
        }

        fun getCancelNotificationPendingIntent(context: Context, id: Long, type: String?): PendingIntent {
            val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
                    .setAction(ACTION_CANCEL)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, id)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE, type)
            return PendingIntentCompat.getBroadcast(context, id.toInt(), intent, 0, false)
        }

        fun onNotificationsComplete(context: Context,
                                     notifications: List<Notification>,
                                     dbWikiSiteMap: Map<String, WikiSite>,
                                     dbWikiNameMap: Map<String, String>) {
            if (Prefs.isSuggestedEditsHighestPriorityEnabled) {
                return
            }

            // The notifications that we need to display are those that don't exist in our db yet.
            val notificationsToDisplay = notifications.filter {
                AppDatabase.instance.notificationDao().getNotificationById(it.wiki, it.id) == null
            }
            AppDatabase.instance.notificationDao().insertNotifications(notificationsToDisplay)

            if (notificationsToDisplay.isNotEmpty()) {
                Prefs.notificationUnreadCount = notificationsToDisplay.size
                WikipediaApp.instance.bus.post(UnreadNotificationsEvent())
            }

            if (notificationsToDisplay.size > 2) {
                // Record that there is an incoming notification to track/compare further actions on it.
                NotificationInteractionEvent.logIncoming(notificationsToDisplay[0], TYPE_MULTIPLE)
                org.wikipedia.analytics.metricsplatform.NotificationInteractionEvent.logIncoming(notificationsToDisplay[0], TYPE_MULTIPLE)
                NotificationPresenter.showMultipleUnread(context, notificationsToDisplay.size)
            } else {
                for (n in notificationsToDisplay) {
                    // Record that there is an incoming notification to track/compare further actions on it.
                    NotificationInteractionEvent.logIncoming(n, null)
                    org.wikipedia.analytics.metricsplatform.NotificationInteractionEvent.logIncoming(n, null)
                    NotificationPresenter.showNotification(context, n,
                        dbWikiNameMap.getOrElse(n.wiki) { n.wiki },
                        dbWikiSiteMap.getValue(n.wiki).languageCode)
                }
            }
        }

        suspend fun markRead(wiki: WikiSite, notifications: List<Notification>, unread: Boolean) {
            withContext(Dispatchers.IO) {
                val token = CsrfTokenClient.getToken(wiki).blockingSingle()
                notifications.windowed(50, partialWindows = true).forEach { window ->
                    val idListStr = window.joinToString("|")
                    ServiceFactory.get(wiki).markRead(token, if (unread) null else idListStr, if (unread) idListStr else null)
                }
            }
        }

        private fun maybeShowLocalNotificationForEditorReactivation(context: Context) {
            if (Prefs.lastDescriptionEditTime == 0L || WikipediaApp.instance.isAnyActivityResumed) {
                return
            }
            var days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.lastDescriptionEditTime)
            if (Prefs.isSuggestedEditsReactivationTestEnabled) {
                days = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Prefs.lastDescriptionEditTime)
            }
            if (days in FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY until SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsReactivationPassStageOne) {
                Prefs.isSuggestedEditsReactivationPassStageOne = true
                showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_one)
            } else if (days >= SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsReactivationPassStageOne) {
                Prefs.isSuggestedEditsReactivationPassStageOne = false
                showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_two)
            }
        }

        fun showSuggestedEditsLocalNotification(context: Context, @StringRes description: Int) {
            val intent = NotificationPresenter.addIntentExtras(MainActivity.newIntent(context).putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, true), 0, TYPE_LOCAL)
            NotificationPresenter.showNotification(context, NotificationPresenter.getDefaultBuilder(context, 0, TYPE_LOCAL), 0,
                    context.getString(R.string.suggested_edits_reactivation_notification_title),
                    context.getString(description), context.getString(description), null,
                    R.drawable.ic_mode_edit_white_24dp, R.color.blue600, intent)
        }
    }
}
