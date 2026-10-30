package org.wikipedia.notifications.experiment

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.wikipedia.util.log.L

class WeeklyMostReadNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            NotificationsExperimentHelper.showNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            L.e(e)
            if (runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "WeeklyMostReadNotificationWorker"
        private const val MAX_RUN_ATTEMPTS = 3

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<WeeklyMostReadNotificationWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }
}
