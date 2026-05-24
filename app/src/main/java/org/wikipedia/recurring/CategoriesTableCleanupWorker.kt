package org.wikipedia.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.log.L
import java.time.LocalDate

class CategoriesTableCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val twoYearsAgoTimeStamp = LocalDate.now().year - CLEANUP_TIME_IN_YEARS
        AppDatabase.instance.categoryDao().deleteOlderThan(twoYearsAgoTimeStamp)
        L.d("Successfully deleted Category data older than $CLEANUP_TIME_IN_YEARS years")
        return Result.success()
    }

    companion object {
        const val CLEANUP_TIME_IN_YEARS = 2
    }
}
