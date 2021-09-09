package org.wikipedia.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.core.app.RemoteInput
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
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
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.events.UnreadNotificationsEvent
import org.wikipedia.main.MainActivity
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
                LOCALLY_KNOWN_NOTIFICATIONS = Prefs.getLocallyKnownNotifications()
                pollNotifications(context)
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
        const val RESULT_EXTRA_ID = "extra_id"
        const val TYPE_MULTIPLE = "multiple"

        private const val TYPE_LOCAL = "local"
        private const val MAX_LOCALLY_KNOWN_NOTIFICATIONS = 32
        private const val FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 3
        private const val SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 7
        private val DBNAME_WIKI_SITE_MAP = mutableMapOf<String, WikiSite>()
        private val DBNAME_WIKI_NAME_MAP = mutableMapOf<String, String>()
        private var LOCALLY_KNOWN_NOTIFICATIONS = Prefs.getLocallyKnownNotifications()

        @JvmStatic
        fun startPollTask(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(),
                        TimeUnit.MINUTES.toMillis((context.resources.getInteger(R.integer.notification_poll_interval_minutes) /
                                if (Prefs.isSuggestedEditsReactivationTestEnabled() && !ReleaseUtil.isDevRelease) 10 else 1).toLong()),
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
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun getCancelNotificationPendingIntent(context: Context, id: Long, type: String?): PendingIntent {
            val intent = Intent(context, NotificationPollBroadcastReceiver::class.java)
                    .setAction(ACTION_CANCEL)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, id)
                    .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE, type)
            return PendingIntent.getBroadcast(context, id.toInt(), intent, 0)
        }

        @SuppressLint("CheckResult")
        @JvmStatic
        fun pollNotifications(context: Context) {
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).lastUnreadNotification
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        var lastNotificationTime = ""
                        for (n in response.query?.notifications?.list.orEmpty()) {
                            if (n.utcIso8601 > lastNotificationTime) {
                                lastNotificationTime = n.utcIso8601
                            }
                        }
                        if (lastNotificationTime <= Prefs.getRemoteNotificationsSeenTime()) {
                            // we're in sync!
                            return@subscribe
                        }
                        Prefs.setRemoteNotificationsSeenTime(lastNotificationTime)
                        retrieveNotifications(context)
                    }) { t ->
                        if (t is MwException && t.error.title == "login-required") {
                            assertLoggedIn()
                        }
                        L.e(t)
                    }
        }

        private fun assertLoggedIn() {
            // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
            // and should automatically log us out if the credentials are no longer valid.
            CsrfTokenClient(WikipediaApp.getInstance().wikiSite).token
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }

        @SuppressLint("CheckResult")
        private fun retrieveNotifications(context: Context) {
            DBNAME_WIKI_SITE_MAP.clear()
            DBNAME_WIKI_NAME_MAP.clear()
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unreadNotificationWikis
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        val wikiMap = response.query!!.unreadNotificationWikis
                        val wikis = mutableListOf<String>()
                        wikis.addAll(wikiMap!!.keys)
                        for (dbName in wikiMap.keys) {
                            if (wikiMap[dbName]!!.source != null) {
                                DBNAME_WIKI_SITE_MAP[dbName] = WikiSite(wikiMap[dbName]!!.source!!.base)
                                DBNAME_WIKI_NAME_MAP[dbName] = wikiMap[dbName]!!.source!!.title
                            }
                        }
                        getFullNotifications(context, wikis)
                    }) { t -> L.e(t) }
        }

        private fun getFullNotifications(context: Context, foreignWikis: List<String?>) {
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).getAllNotifications(if (foreignWikis.isEmpty()) "*" else foreignWikis.joinToString("|"), "!read", null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response -> onNotificationsComplete(context, response.query?.notifications?.list) }) { t -> L.e(t) }
        }

        private fun onNotificationsComplete(context: Context, notifications: List<Notification>?) {
            if (notifications.isNullOrEmpty() || Prefs.isSuggestedEditsHighestPriorityEnabled()) {
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
                Prefs.setNotificationUnreadCount(notificationsToDisplay.size)
                WikipediaApp.getInstance().bus.post(UnreadNotificationsEvent())
            }

            if (notificationsToDisplay.size > 2) {
                // Record that there is an incoming notification to track/compare further actions on it.
                NotificationInteractionFunnel(WikipediaApp.getInstance(), 0, notificationsToDisplay[0].wiki, TYPE_MULTIPLE).logIncoming()
                NotificationInteractionEvent.logIncoming(notificationsToDisplay[0], TYPE_MULTIPLE)
                NotificationPresenter.showMultipleUnread(context, notificationsToDisplay.size)
            } else {
                for (n in notificationsToDisplay) {
                    // Record that there is an incoming notification to track/compare further actions on it.
                    NotificationInteractionFunnel(WikipediaApp.getInstance(), n).logIncoming()
                    NotificationInteractionEvent.logIncoming(n, null)
                    NotificationPresenter.showNotification(context, n, (if (DBNAME_WIKI_NAME_MAP.containsKey(n.wiki)) DBNAME_WIKI_NAME_MAP[n.wiki] else n.wiki)!!)
                }
            }
            if (locallyKnownModified) {
                Prefs.setLocallyKnownNotifications(LOCALLY_KNOWN_NOTIFICATIONS)
            }
            if (knownNotifications.size > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
                markItemsAsRead(knownNotifications.subList(0, knownNotifications.size - MAX_LOCALLY_KNOWN_NOTIFICATIONS))
            }
        }

        private fun markItemsAsRead(items: List<Notification>) {
            val notificationsPerWiki = mutableMapOf<WikiSite, MutableList<Notification>>()
            for (item in items) {
                val wiki = DBNAME_WIKI_SITE_MAP.getOrElse(item.wiki) { WikipediaApp.getInstance().wikiSite }
                notificationsPerWiki.getOrPut(wiki) { mutableListOf() }.add(item)
            }
            for (wiki in notificationsPerWiki.keys) {
                markRead(wiki, notificationsPerWiki[wiki]!!, false)
            }
        }

        fun markRead(wiki: WikiSite, notifications: List<Notification>, unread: Boolean) {
            val idListStr = notifications.joinToString("|")
            CsrfTokenClient(wiki, WikipediaApp.getInstance().wikiSite).token
                    .subscribeOn(Schedulers.io())
                    .flatMap {
                        ServiceFactory.get(wiki).markRead(it, if (unread) null else idListStr, if (unread) idListStr else null)
                                .subscribeOn(Schedulers.io())
                    }
                    .subscribe({ }, { L.e(it) })
        }

        private fun maybeShowLocalNotificationForEditorReactivation(context: Context) {
            if (Prefs.getLastDescriptionEditTime() == 0L || WikipediaApp.getInstance().isAnyActivityResumed) {
                return
            }
            var days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.getLastDescriptionEditTime())
            if (Prefs.isSuggestedEditsReactivationTestEnabled()) {
                days = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Prefs.getLastDescriptionEditTime())
            }
            if (days in FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY until SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && !Prefs.isSuggestedEditsReactivationPassStageOne()) {
                Prefs.setSuggestedEditsReactivationPassStageOne(true)
                showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_one)
            } else if (days >= SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsReactivationPassStageOne()) {
                Prefs.setSuggestedEditsReactivationPassStageOne(false)
                showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_two)
            }
        }

        fun showSuggestedEditsLocalNotification(context: Context, @StringRes description: Int) {
            val intent = NotificationPresenter.addIntentExtras(MainActivity.newIntent(context).putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, true), 0, TYPE_LOCAL)
            NotificationPresenter.showNotification(context, NotificationPresenter.getDefaultBuilder(context, 0, TYPE_LOCAL), 0,
                    context.getString(R.string.suggested_edits_reactivation_notification_title),
                    context.getString(description), context.getString(description),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, false, intent)
        }
    }
}
