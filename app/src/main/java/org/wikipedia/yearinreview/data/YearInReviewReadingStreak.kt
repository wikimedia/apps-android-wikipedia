package org.wikipedia.yearinreview.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class YearInReviewReadingStreak(
    val start: LocalDate,
    val end: LocalDate
) {
    val days get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    companion object {
        // The longest run of consecutive days, where a tie goes to the more recent run. Null when there are no days.
        fun longestOf(distinctSortedDays: List<LocalDate>): YearInReviewReadingStreak? {
            val streaks = mutableListOf<YearInReviewReadingStreak>()
            distinctSortedDays.forEach { day ->
                val lastStreak = streaks.lastOrNull()
                if (lastStreak != null && lastStreak.end.plusDays(1) == day) {
                    streaks[streaks.lastIndex] = lastStreak.copy(end = day)
                } else {
                    streaks.add(YearInReviewReadingStreak(start = day, end = day))
                }
            }
            return streaks.asReversed().maxByOrNull { it.days }
        }
    }
}
