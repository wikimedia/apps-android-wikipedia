package org.wikipedia.recurring

import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date

class RecommendedReadingListTask : RecurringTask() {
    override val name = "generateRecommendedReadingListTask"

    override fun shouldRun(lastRun: Date): Boolean {
        val lastRunDate = lastRun.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return isUpdateDue(lastRunDate, LocalDate.now(), Prefs.recommendedReadingListUpdateFrequency)
    }

    override suspend fun run(lastRun: Date) {
        RecommendedReadingListHelper.generateRecommendedReadingList(true)
    }

    companion object {
        /**
         * The list is due when it hasn't been updated since the start of the current period (today, this Monday,
         * or the first day of this month), so an update day the user missed is caught up the next time the app is opened.
         */
        internal fun isUpdateDue(lastRunDate: LocalDate, today: LocalDate, frequency: RecommendedReadingListUpdateFrequency): Boolean {
            val startOfCurrentPeriod = when (frequency) {
                RecommendedReadingListUpdateFrequency.DAILY -> today
                RecommendedReadingListUpdateFrequency.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                RecommendedReadingListUpdateFrequency.MONTHLY -> today.withDayOfMonth(1)
            }
            return lastRunDate.isBefore(startOfCurrentPeriod)
        }
    }
}
