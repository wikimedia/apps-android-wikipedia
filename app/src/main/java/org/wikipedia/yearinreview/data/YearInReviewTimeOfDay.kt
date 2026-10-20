package org.wikipedia.yearinreview.data

import org.wikipedia.R
import org.wikipedia.history.db.HourVisitCount
import kotlin.math.roundToInt

enum class YearInReviewTimeOfDay(val hours: IntRange, val resId: Int) {
    MORNING(5..11, R.string.year_in_review_slide_reading_pattern_morning),
    MIDDAY(12..12, R.string.year_in_review_slide_reading_pattern_midday),
    AFTERNOON(13..16, R.string.year_in_review_slide_reading_pattern_afternoon),
    EVENING(17..20, R.string.year_in_review_slide_reading_pattern_evening),
    NIGHT(21..23, R.string.year_in_review_slide_reading_pattern_night),
    LATE_NIGHT(0..4, R.string.year_in_review_slide_reading_pattern_late_night);

    companion object {
        const val MIN_VISITS = 2

        fun ofHour(hour: Int) = entries.first { hour in it.hours }

        // The time of day with the most visits, paired with its rounded percentage of all visits.
        // Null when no time of day has at least MIN_VISITS visits.
        fun favoriteOf(visitCountsByHour: List<HourVisitCount>): Pair<YearInReviewTimeOfDay, Int>? {
            val (favoriteTimeOfDay, favoriteVisits) = visitCountsByHour
                .groupingBy { ofHour(it.hour) }
                .fold(0) { visits, hourVisitCount -> visits + hourVisitCount.visitCount }
                .maxByOrNull { it.value } ?: return null
            if (favoriteVisits < MIN_VISITS) {
                return null
            }
            val totalVisits = visitCountsByHour.sumOf { it.visitCount }
            return favoriteTimeOfDay to (favoriteVisits * 100.0 / totalVisits).roundToInt()
        }
    }
}
