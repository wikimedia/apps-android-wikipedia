package org.wikipedia.recurring

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.wikipedia.R
import org.wikipedia.alphaupdater.AlphaUpdateWorker
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.settings.RemoteConfigRefreshWorker
import org.wikipedia.util.ReleaseUtil
import java.util.concurrent.TimeUnit

object RecurringTasksExecutor {
    fun scheduleTasks(context: Context) {
        // Clean up now-unused task time values, as WorkManager handles scheduling
        PrefsIoUtil.remove("alpha-update-checker")
        PrefsIoUtil.remove("remote-config-refresher")
        PrefsIoUtil.remove(R.string.preference_key_talk_offline_cleanup_task_name)
        PrefsIoUtil.remove(R.string.preference_key_daily_event_time_task_name)

        val networkConstraints = Constraints(NetworkType.CONNECTED)

        val remoteConfigRefreshRequest = PeriodicWorkRequestBuilder<RemoteConfigRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints)
            .build()

        val dailyEventRequest = PeriodicWorkRequestBuilder<DailyEventWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraints)
            .build()

        val offlineCleanupRequest = PeriodicWorkRequestBuilder<TalkOfflineCleanupWorker>(7, TimeUnit.DAYS)
            .build()

        val taskNames = mutableListOf("REMOTE_CONFIG", "DAILY_EVENT", "OFFLINE_CLEANUP")
        val tasks = mutableListOf(remoteConfigRefreshRequest, dailyEventRequest, offlineCleanupRequest)

        if (ReleaseUtil.isAlphaRelease) {
            taskNames.add("ALPHA_UPDATE")

            val alphaUpdateRequest = PeriodicWorkRequestBuilder<AlphaUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints)
                .build()

            tasks.add(alphaUpdateRequest)
        }

        (taskNames zip tasks).forEach { (taskName, task) ->
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(taskName, ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }
}
