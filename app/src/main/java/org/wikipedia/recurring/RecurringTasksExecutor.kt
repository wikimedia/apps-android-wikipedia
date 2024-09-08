package org.wikipedia.recurring

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.alphaupdater.AlphaUpdateWorker
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.settings.RemoteConfigRefreshWorker
import org.wikipedia.util.ReleaseUtil
import java.util.concurrent.TimeUnit

object RecurringTasksExecutor {
    fun schedule() {
        // Clean up now-unused task time values, as WorkManager handles scheduling
        PrefsIoUtil.remove("alpha-update-checker")
        PrefsIoUtil.remove("remote-config-refresher")
        PrefsIoUtil.remove(R.string.preference_key_talk_offline_cleanup_task_name)
        PrefsIoUtil.remove(R.string.preference_key_daily_event_time_task_name)

        val networkConstraints = Constraints(
            requiredNetworkType = NetworkType.CONNECTED,
            requiresBatteryNotLow = true
        )

        val remoteConfigRefreshRequest = PeriodicWorkRequestBuilder<RemoteConfigRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints)
            .build()

        val dailyEventRequest = PeriodicWorkRequestBuilder<DailyEventWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints)
            .build()

        val offlineCleanupRequest = PeriodicWorkRequestBuilder<TalkOfflineCleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints(requiresBatteryNotLow = true))
            .build()

        val tasks = mutableMapOf(
            "REMOTE_CONFIG" to remoteConfigRefreshRequest,
            "DAILY_EVENT" to dailyEventRequest,
            "OFFLINE_CLEANUP" to offlineCleanupRequest
        )

        if (ReleaseUtil.isAlphaRelease) {
            tasks["ALPHA_UPDATE"] = PeriodicWorkRequestBuilder<AlphaUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints)
                .build()
        }

        tasks.forEach { (taskName, task) ->
            WorkManager.getInstance(WikipediaApp.instance)
                .enqueueUniquePeriodicWork(taskName, ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }
}
