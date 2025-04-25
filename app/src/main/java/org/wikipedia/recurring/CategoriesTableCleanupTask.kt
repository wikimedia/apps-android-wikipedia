package org.wikipedia.recurring

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class CategoriesTableCleanupTask : RecurringTask() {
    private val TAG = this.javaClass.simpleName

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(DAYS_BETWEEN_RUNS)
    }

    override suspend fun run(lastRun: Date) {
        withContext(Dispatchers.IO) {
            val twoYearsAgoTimeStamp = getTimeStampForYearsAgo(CLEANUP_TIME_IN_YEARS)
            try {
                AppDatabase.instance.categoryDao().deleteOlderThan(twoYearsAgoTimeStamp)
                Log.d(TAG, "Successfully deleted Category data older than two years")
            } catch (e: Exception) {
                Log.e(TAG, "error: ${e.message}")
            }
        }
    }

    override val name: String = "CategoriesTableCleanupTask"

    private fun getTimeStampForYearsAgo(years: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -years)
        return calendar.timeInMillis
    }

    companion object {
        private const val CLEANUP_TIME_IN_YEARS = 2
        private const val DAYS_BETWEEN_RUNS = (365L / 2L)
    }
}
