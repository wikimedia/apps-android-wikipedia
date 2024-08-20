package org.wikipedia.recurring

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.PrefsIoUtil
import java.util.concurrent.TimeUnit

object RecurringTasksExecutor {
    fun run() {
        // Clean up now-unused task time values, as WorkManager handles scheduling
        PrefsIoUtil.remove("alpha-update-checker")
        PrefsIoUtil.remove("remote-config-refresher")
        PrefsIoUtil.remove(R.string.preference_key_talk_offline_cleanup_task_name)
        PrefsIoUtil.remove(R.string.preference_key_daily_event_time_task_name)

        val offlineCleanupRequest = PeriodicWorkRequestBuilder<TalkOfflineCleanupWorker>(7, TimeUnit.DAYS)
            .build()

        val taskNames = mutableListOf("OFFLINE_CLEANUP")
        val tasks = mutableListOf(offlineCleanupRequest)

        (taskNames zip tasks).forEach { (taskName, task) ->
            WorkManager.getInstance(WikipediaApp.instance)
                .enqueueUniquePeriodicWork(taskName, ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }
}
