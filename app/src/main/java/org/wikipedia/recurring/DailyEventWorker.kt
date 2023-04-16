package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.util.log.L

class DailyEventWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        DailyStatsEvent.log(WikipediaApp.instance)

        return try {
            EventPlatformClient.refreshStreamConfigsSuspend()
            Result.success()
        } catch (e: Exception) {
            L.e(e)
            Result.failure()
        }
    }
}
