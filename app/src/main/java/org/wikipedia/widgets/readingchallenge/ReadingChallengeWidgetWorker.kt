package org.wikipedia.widgets.readingchallenge

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReadingChallengeWidgetWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        ReadingChallengeWidget().updateAll(applicationContext)
        scheduleNextMidnightUpdate(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "ReadingChallengeWidgetWorker"

        fun scheduleNextMidnightUpdate(context: Context) {
            val delay = if (ReleaseUtil.isPreBetaRelease && Prefs.readingChallengeWidgetFastCycle) {
                Duration.ofMinutes(1)
            } else {
                val now = LocalDateTime.now()
                val nextMidnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1)
                Duration.between(now, nextMidnight)
            }
            val workRequest = OneTimeWorkRequest.Builder(ReadingChallengeWidgetWorker::class.java)
                .addTag(ReadingChallengeWidgetWorker::class.java.simpleName)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelScheduledUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
