package org.wikipedia.notifications

import android.content.Context
import androidx.work.*
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.time.Instant

class PollNotificationWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).lastUnreadNotification()
            val lastNotificationTime = response.query?.notifications?.list
                ?.maxOfOrNull { it.instant ?: Instant.EPOCH } ?: Instant.EPOCH
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
        val dbWikiSiteMap = mutableMapOf<String, WikiSite>().withDefault { WikipediaApp.instance.wikiSite }
        val dbWikiNameMap = mutableMapOf<String, String>()
        val wikiMap = ServiceFactory.get(WikipediaApp.instance.wikiSite).unreadNotificationWikis()
            .query!!.unreadNotificationWikis.orEmpty()
        val foreignWikis = wikiMap.keys.toList()
        for ((dbName, wiki) in wikiMap) {
            if (wiki.source != null) {
                dbWikiSiteMap[dbName] = WikiSite(wiki.source.base)
                dbWikiNameMap[dbName] = wiki.source.title
            }
        }

        ServiceFactory.get(WikipediaApp.instance.wikiSite)
            .getAllNotifications(if (foreignWikis.isEmpty()) "*" else foreignWikis.joinToString("|"), "!read", null)
            .query?.notifications?.list?.let {
                NotificationPollBroadcastReceiver.onNotificationsComplete(appContext, it, dbWikiSiteMap, dbWikiNameMap)
            }
    }

    private fun assertLoggedIn() {
        // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
        // and should automatically log us out if the credentials are no longer valid.
        CsrfTokenClient.getToken(WikipediaApp.instance.wikiSite)
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
