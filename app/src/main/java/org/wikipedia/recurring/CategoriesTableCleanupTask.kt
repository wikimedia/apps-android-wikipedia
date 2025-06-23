package org.wikipedia.recurring

import org.wikipedia.database.AppDatabase
import org.wikipedia.util.log.L
import java.time.LocalDateTime
import java.util.Date
import java.util.concurrent.TimeUnit

class CategoriesTableCleanupTask : RecurringTask() {
    override val name = "categoriesTableCleanupTask"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(DAYS_BETWEEN_RUNS)
    }

    override suspend fun run(lastRun: Date) {
        val twoYearsAgoTimeStamp = LocalDateTime.now().year - CLEANUP_TIME_IN_YEARS
        deleteOldDataInBatches(twoYearsAgoTimeStamp)
        L.d("Successfully deleted Category data older than $CLEANUP_TIME_IN_YEARS years")
    }

    suspend fun deleteOldDataInBatches(year: Int) {
        AppDatabase.instance.categoryDao().deleteOlderThanInBatch(year)
    }

    companion object {
        private const val CLEANUP_TIME_IN_YEARS = 2
        private const val DAYS_BETWEEN_RUNS = (365L / 2L)
    }
}
