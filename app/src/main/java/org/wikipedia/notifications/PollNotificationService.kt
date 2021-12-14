package org.wikipedia.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class PollNotificationService : JobService() {

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        pollNotifications(this, jobParameters)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        return true
    }

    private fun pollNotifications(context: Context, jobParameters: JobParameters?) {
        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, t ->
            if (t is MwException && t.error.title == "login-required") {
                assertLoggedIn()
            }
            L.e(t)
            jobFinished(jobParameters, false)
        }) {
            val response = ServiceFactory.get(WikipediaApp.getInstance().wikiSite).lastUnreadNotification()
            val lastNotificationTime =
                response.query?.notifications?.list?.maxOfOrNull { it.utcIso8601 }.orEmpty()
            if (lastNotificationTime > Prefs.remoteNotificationsSeenTime) {
                Prefs.remoteNotificationsSeenTime = lastNotificationTime
                retrieveNotifications(context)
            }
            jobFinished(jobParameters, false)
        }
    }

    private suspend fun retrieveNotifications(context: Context) {
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

    private fun assertLoggedIn() {
        // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
        // and should automatically log us out if the credentials are no longer valid.
        CsrfTokenClient(WikipediaApp.getInstance().wikiSite).token
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    companion object {

        fun schedulePollNotificationJob(context: Context) {
            val serviceComponent = ComponentName(context, PollNotificationService::class.java)
            val builder = JobInfo.Builder(0, serviceComponent)
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }
}
