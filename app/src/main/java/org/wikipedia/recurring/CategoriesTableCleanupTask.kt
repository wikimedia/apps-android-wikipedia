package org.wikipedia.recurring

import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.log.L
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class CategoriesTableCleanupTask(app: WikipediaApp) : RecurringTask() {
    override val name = "categoriesTableCleanupTask"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(DAYS_BETWEEN_RUNS)
    }

    override suspend fun run(lastRun: Date) {
        val twoYearsAgoTimeStamp = getTimeStampForYearsAgo()
        deleteOldDataInBatches(twoYearsAgoTimeStamp, 1000)
        L.d("Successfully deleted Category data older than $CLEANUP_TIME_IN_YEARS years")
    }

    private fun getTimeStampForYearsAgo(years: Int = CLEANUP_TIME_IN_YEARS): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -years)
        return calendar.timeInMillis
    }

    suspend fun deleteOldDataInBatches(timeStamp: Long, batchSize: Int) {
        do {
            val deletedCount = AppDatabase.instance.categoryDao().deleteOlderThanInBatch(timeStamp, batchSize)
        } while (deletedCount > 0)
    }

    companion object {
        private const val CLEANUP_TIME_IN_YEARS = 2
        private const val DAYS_BETWEEN_RUNS = (365L / 2L)
    }
}
