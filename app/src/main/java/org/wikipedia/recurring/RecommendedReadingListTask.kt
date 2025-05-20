package org.wikipedia.recurring

import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.readinglist.recommended.RecommendedReadingListViewModel
import org.wikipedia.settings.Prefs
import java.util.Date
import java.util.concurrent.TimeUnit

class RecommendedReadingListTask() : RecurringTask() {
    override val name = "generateRecommendedReadingListTask"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= when (Prefs.recommendedReadingListUpdateFrequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> TimeUnit.DAYS.toMillis(1)
            RecommendedReadingListUpdateFrequency.WEEKLY -> TimeUnit.DAYS.toMillis(7)
            RecommendedReadingListUpdateFrequency.MONTHLY -> TimeUnit.DAYS.toMillis(31)
        }
    }

    override suspend fun run(lastRun: Date) {
        RecommendedReadingListViewModel.generateRecommendedReadingList()
    }
}
