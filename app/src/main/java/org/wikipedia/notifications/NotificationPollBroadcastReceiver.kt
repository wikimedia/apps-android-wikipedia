package org.wikipedia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.app.RemoteInput
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.UnreadNotificationsEvent
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.db.Notification
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.settings.Prefs
import org.wikipedia.talk.NotificationDirectReplyHelper
import org.wikipedia.util.DeviceUtil
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
                LOCALLY_KNOWN_NOTIFICATIONS = Prefs.locallyKnownNotifications.toMutableList()
                PollNotificationWorker.schedulePollNotificationJob(context)
            }
            ACTION_CANCEL == intent.action -> {
                NotificationInteractionFunnel.processIntent(intent)
            }
            ACTION_DIRECT_REPLY == intent.action -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val text = remoteInput.getCharSequence(RESULT_KEY_DIRECT_REPLY)

                if (intent.hasExtra(RESULT_EXTRA_WIKI) && intent.hasExtra(RESULT_EXTRA_TITLE) && !text.isNullOrEmpty()) {
                    NotificationDirectReplyHelper.handleReply(context,
                        intent.getParcelableExtra(RESULT_EXTRA_WIKI)!!,
                        intent.getParcelableExtra(RESULT_EXTRA_TITLE)!!,
                        text.toString(),
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
        const val RESULT_EXTRA_WIKI = "extra_wiki"
        const val RESULT_EXTRA_TITLE = "extra_title"
        const val RESULT_EXTRA_REPLY_TO = "extra_reply_to"
        const val RESULT_EXTRA_ID = "extra_id"
        const val TYPE_MULTIPLE = "multiple"

        private const val TYPE_LOCAL = "local"
        private const val MAX_LOCALLY_KNOWN_NOTIFICATIONS = 32
        private const val FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 3
        private const val SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 7
        val DBNAME_WIKI_SITE_MAP = mutableMapOf<String, WikiSite>()
        val DBNAME_WIKI_NAME_MAP = mutableMapOf<String, String>()
        private var LOCALLY_KNOWN_NOTIFICATIONS = Prefs.locallyKnownNotifications.toMutableList()

        @JvmStatic
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
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or DeviceUtil.pendingIntentFlags)
        }

        fun getCancelNotificationPendingIntent(context: Context, id: Long, type: String?): PendingIntent {
            val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
                    .setAction(ACTION_CANCEL)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, id)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE, type)
            return PendingIntent.getBroadcast(context, id.toInt(), intent, DeviceUtil.pendingIntentFlags)
        }

         fun onNotificationsComplete(context: Context, notifications: List<Notification>) {
            if (Prefs.isSuggestedEditsHighestPriorityEnabled) {
                return
            }
            var locallyKnownModified = false
            val knownNotifications = mutableListOf<Notification>()
            val notificationsToDisplay = mutableListOf<Notification>()
            for (n in notifications) {
                knownNotifications.add(n)
                if (LOCALLY_KNOWN_NOTIFICATIONS.contains(n.key())) {
                    continue
                }
                LOCALLY_KNOWN_NOTIFICATIONS.add(n.key())
                if (LOCALLY_KNOWN_NOTIFICATIONS.size > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
                    LOCALLY_KNOWN_NOTIFICATIONS.removeAt(0)
                }
                notificationsToDisplay.add(n)
                locallyKnownModified = true
            }
            if (notificationsToDisplay.isNotEmpty()) {
                Prefs.notificationUnreadCount = notificationsToDisplay.size
                WikipediaApp.getInstance().bus.post(UnreadNotificationsEvent())
            }

            // Android 7.0 and above performs automatic grouping of multiple notifications, in case
            // there are significantly more than one. But in the case of Android 6.0 and below,
            // we show our own custom "grouped" notification.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && notificationsToDisplay.size > 2) {
                // Record that there is an incoming notification to track/compare further actions on it.
                NotificationInteractionFunnel(WikipediaApp.getInstance(), 0, notificationsToDisplay[0].wiki, TYPE_MULTIPLE).logIncoming()
                NotificationInteractionEvent.logIncoming(notificationsToDisplay[0], TYPE_MULTIPLE)
                NotificationPresenter.showMultipleUnread(context, notificationsToDisplay.size)
            } else {
                for (n in notificationsToDisplay) {
                    // Record that there is an incoming notification to track/compare further actions on it.
                    NotificationInteractionFunnel(WikipediaApp.getInstance(), n).logIncoming()
                    NotificationInteractionEvent.logIncoming(n, null)
                    NotificationPresenter.showNotification(context, n,
                        DBNAME_WIKI_NAME_MAP.getOrElse(n.wiki) { n.wiki },
                        DBNAME_WIKI_SITE_MAP.getOrElse(n.wiki) { WikipediaApp.getInstance().wikiSite }.languageCode)
                }
            }
            if (locallyKnownModified) {
                Prefs.locallyKnownNotifications = LOCALLY_KNOWN_NOTIFICATIONS
            }
            if (knownNotifications.size > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
                markItemsAsRead(knownNotifications.subList(0, knownNotifications.size - MAX_LOCALLY_KNOWN_NOTIFICATIONS))
            }
        }

        private fun markItemsAsRead(items: List<Notification>) {
            val notificationsPerWiki = items.groupBy {
                DBNAME_WIKI_SITE_MAP.getOrElse(it.wiki) { WikipediaApp.getInstance().wikiSite }
            }
            for ((wiki, notifications) in notificationsPerWiki) {
                markRead(wiki, notifications, false)
            }
        }

        fun markRead(wiki: WikiSite, notifications: List<Notification>, unread: Boolean) {
            val idListStr = notifications.joinToString("|")
            CsrfTokenClient(wiki).token
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        ServiceFactory.get(wiki).markRead(it, if (unread) null else idListStr, if (unread) idListStr else null)
                                .subscribeOn(Schedulers.io())
                    }
                    .subscribe({ }, { L.e(it) })
        }

        private fun maybeShowLocalNotificationForEditorReactivation(context: Context) {
            if (Prefs.lastDescriptionEditTime == 0L || WikipediaApp.getInstance().isAnyActivityResumed) {
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
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent)
        }
    }
}
