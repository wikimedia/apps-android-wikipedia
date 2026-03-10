package org.wikipedia.widgets.readingchallenge

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import org.wikipedia.widgets.readingchallenge.largewidget.ReadingChallengeLargeWidget
import org.wikipedia.widgets.readingchallenge.largewidget.ReadingChallengeLargeWidgetReceiver
import org.wikipedia.widgets.readingchallenge.smallwidget.ReadingChallengeSmallWidget
import org.wikipedia.widgets.readingchallenge.smallwidget.ReadingChallengeSmallWidgetReceiver
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReadingChallengeWidgetWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val smallWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, ReadingChallengeSmallWidgetReceiver::class.java)
        )
        val largeWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, ReadingChallengeLargeWidgetReceiver::class.java)
        )

        if (smallWidgetIds.isNotEmpty()) {
            ReadingChallengeSmallWidget().updateAll(applicationContext)
        }

        if (largeWidgetIds.isNotEmpty()) {
            ReadingChallengeLargeWidget().updateAll(applicationContext)
        }

        scheduleNextMidnightUpdate(applicationContext)

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "ReadingChallengeWidgetWorker"

        fun scheduleNextMidnightUpdate(context: Context) {
            val now = LocalDateTime.now()
            val nextMidnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1)
            val delay = Duration.between(now, nextMidnight)

            val workRequest = OneTimeWorkRequest.Builder(ReadingChallengeWidgetWorker::class.java)
                .addTag(ReadingChallengeWidgetWorker::class.java.simpleName)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            Log.d("WidgetDebug", "scheduled for $nextMidnight")
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
