package org.wikipedia.notifications

import android.content.Context
import androidx.work.*
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class PollNotificationWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).lastUnreadNotification()
            val lastNotificationTime = response.query?.notifications?.list?.maxOfOrNull { it.utcIso8601 }.orEmpty()
            if (lastNotificationTime > Prefs.remoteNotificationsSeenTime) {
                Prefs.remoteNotificationsSeenTime = lastNotificationTime
                retrieveNotifications()
            }
            Result.success()
        } catch (t: Throwable) {
            if (t is MwException && t.error.title == "login-required") {
                assertLoggedIn()
            }
            L.e(t)
            Result.failure()
        }
    }

    private suspend fun retrieveNotifications() {
        NotificationPollBroadcastReceiver.DBNAME_WIKI_SITE_MAP.clear()
        NotificationPollBroadcastReceiver.DBNAME_WIKI_NAME_MAP.clear()
        val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).unreadNotificationWikis()
        val wikiMap = response.query!!.unreadNotificationWikis.orEmpty()
        val wikis = wikiMap.keys.toList()
        for ((dbName, wiki) in wikiMap) {
            if (wiki.source != null) {
                NotificationPollBroadcastReceiver.DBNAME_WIKI_SITE_MAP[dbName] = WikiSite(wiki.source.base)
                NotificationPollBroadcastReceiver.DBNAME_WIKI_NAME_MAP[dbName] = wiki.source.title
            }
        }
        getFullNotifications(wikis)
    }

    private suspend fun getFullNotifications(foreignWikis: List<String>) {
        val notificationRepository = NotificationRepository(AppDatabase.instance.notificationDao())

        val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite)
            .getAllNotifications(if (foreignWikis.isEmpty()) "*" else foreignWikis.joinToString("|"), "!read", null)
        response.query?.notifications?.list?.let {
            notificationRepository.insertNotifications(it)
            NotificationPollBroadcastReceiver.onNotificationsComplete(appContext, it)
        }
    }

    private fun assertLoggedIn() {
        // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
        // and should automatically log us out if the credentials are no longer valid.
        CsrfTokenClient(WikipediaApp.getInstance().wikiSite).token
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    companion object {
        fun schedulePollNotificationJob(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<PollNotificationWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
