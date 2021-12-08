package org.wikipedia.notifications

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class PollNotificationService : JobService() {

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        pollNotifications(this)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        return true
    }

    private fun pollNotifications(context: Context) {
        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, t ->
            if (t is MwException && t.error.title == "login-required") {
                NotificationPollBroadcastReceiver.assertLoggedIn()
            }
            L.e(t)
        }) {
            val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).lastUnreadNotification()
            val lastNotificationTime =
                response.query?.notifications?.list?.maxOfOrNull { it.utcIso8601 }.orEmpty()
            if (lastNotificationTime > Prefs.remoteNotificationsSeenTime) {
                Prefs.remoteNotificationsSeenTime = lastNotificationTime
                retrieveNotifications(context)
            }
        }
    }

    suspend fun retrieveNotifications(context: Context) {
        NotificationPollBroadcastReceiver.DBNAME_WIKI_SITE_MAP.clear()
        NotificationPollBroadcastReceiver.DBNAME_WIKI_NAME_MAP.clear()
        val response =
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unreadNotificationWikis()
        val wikiMap = response.query!!.unreadNotificationWikis
        val wikis = mutableListOf<String>()
        wikis.addAll(wikiMap!!.keys)
        for (dbName in wikiMap.keys) {
            if (wikiMap[dbName]!!.source != null) {
                NotificationPollBroadcastReceiver.DBNAME_WIKI_SITE_MAP[dbName] =
                    WikiSite(wikiMap[dbName]!!.source!!.base)
                NotificationPollBroadcastReceiver.DBNAME_WIKI_NAME_MAP[dbName] =
                    wikiMap[dbName]!!.source!!.title
            }
        }
        getFullNotifications(context, wikis)
    }

    private suspend fun getFullNotifications(context: Context, foreignWikis: List<String?>) {
        val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())

        val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite)
            .getAllNotifications(if (foreignWikis.isEmpty()) "*" else foreignWikis.joinToString("|"), "!read", null)
        response.query?.notifications?.list?.let {
            notificationRepository.insertNotifications(it)
            NotificationPollBroadcastReceiver.onNotificationsComplete(context, it)
        }
    }
}
