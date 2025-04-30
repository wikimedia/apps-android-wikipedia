package org.wikipedia.recurring

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.log.L
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class CategoriesTableCleanupTask(app: WikipediaApp) : RecurringTask() {
    override val name: String = app.getString(R.string.preference_key_categories_table_cleanup_task_name)

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(DAYS_BETWEEN_RUNS)
    }

    override suspend fun run(lastRun: Date) {
        val twoYearsAgoTimeStamp = getTimeStampForYearsAgo()
        try {
            AppDatabase.instance.categoryDao().deleteOlderThan(twoYearsAgoTimeStamp)
            L.d("Successfully deleted Category data older than $CLEANUP_TIME_IN_YEARS years")
        } catch (e: Exception) {
            L.e(e)
        }
    }

    private fun getTimeStampForYearsAgo(years: Int = CLEANUP_TIME_IN_YEARS): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -years)
        return calendar.timeInMillis
    }

    companion object {
        private const val CLEANUP_TIME_IN_YEARS = 2
        private const val DAYS_BETWEEN_RUNS = (365L / 2L)
    }
}
