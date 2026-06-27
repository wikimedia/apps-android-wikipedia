package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient

class DailyEventWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        DailyStatsEvent.log(WikipediaApp.instance)
        EventPlatformClient.refreshStreamConfigs()
        return Result.success()
    }
}
