package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import java.time.DayOfWeek
import java.time.LocalDate

class RecommendedReadingListWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // And run either every day, or on the first day of the week or month
        val now = LocalDate.now()
        val shouldRun = when (Prefs.recommendedReadingListUpdateFrequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> true
            RecommendedReadingListUpdateFrequency.WEEKLY -> now.dayOfWeek == DayOfWeek.MONDAY
            RecommendedReadingListUpdateFrequency.MONTHLY -> now.dayOfMonth == 1
        }
        if (shouldRun) {
            RecommendedReadingListHelper.generateRecommendedReadingList(true)
        }
        return Result.success()
    }
}
