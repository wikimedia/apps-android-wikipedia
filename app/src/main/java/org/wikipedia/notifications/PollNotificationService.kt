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

    private var jobParameters: JobParameters? = null

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        pollNotifications(this)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        jobParameters = p0
        return true
    }

    private fun pollNotifications(context: Context) {
        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, t ->
            if (t is MwException && t.error.title == "login-required") {
                assertLoggedIn()
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

    private suspend fun retrieveNotifications(context: Context) {
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

    private fun assertLoggedIn() {
        // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
        // and should automatically log us out if the credentials are no longer valid.
        CsrfTokenClient(WikipediaApp.getInstance().wikiSite).token
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    companion object {
        var pollNotificationService = PollNotificationService()

        fun schedulePollNotificationJob(context: Context) {
            val serviceComponent = ComponentName(context, pollNotificationService.javaClass)
            val builder = JobInfo.Builder(0, serviceComponent)
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
         fun pollNotificationJobFinished() {
            pollNotificationService.jobFinished(pollNotificationService.jobParameters, false)
        }
    }
}
