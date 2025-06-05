package org.wikipedia.recurring

import org.wikipedia.readinglist.recommended.RecommendedReadingListAbTest
import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.TimeUnit

class RecommendedReadingListTask() : RecurringTask() {
    override val name = "generateRecommendedReadingListTask"

    override fun shouldRun(lastRun: Date): Boolean {
        // should be part of the test group or at least one day has passed
        if (!RecommendedReadingListAbTest().isTestGroupUser() || millisSinceLastRun(lastRun) < TimeUnit.DAYS.toMillis(1)) {
            return false
        }
        // And run either every day, or on the first day of the week or month
        val now = LocalDate.now()
        return when (Prefs.recommendedReadingListUpdateFrequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> true
            RecommendedReadingListUpdateFrequency.WEEKLY -> now.dayOfWeek.value == 1
            RecommendedReadingListUpdateFrequency.MONTHLY -> now.dayOfMonth == 1
        }
    }

    override suspend fun run(lastRun: Date) {
        RecommendedReadingListHelper.generateRecommendedReadingList()
    }
}
