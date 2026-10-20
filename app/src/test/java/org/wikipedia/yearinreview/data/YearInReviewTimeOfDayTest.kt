package org.wikipedia.yearinreview.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.wikipedia.history.db.HourVisitCount

class YearInReviewTimeOfDayTest {

    @Test
    fun `every hour belongs to exactly one time of day`() {
        (0..23).forEach { hour ->
            assertEquals(1, YearInReviewTimeOfDay.entries.count { hour in it.hours })
        }
    }

    @Test
    fun `hours at the edges of each time of day`() {
        assertEquals(YearInReviewTimeOfDay.LATE_NIGHT, YearInReviewTimeOfDay.ofHour(4))
        assertEquals(YearInReviewTimeOfDay.MORNING, YearInReviewTimeOfDay.ofHour(5))
        assertEquals(YearInReviewTimeOfDay.MORNING, YearInReviewTimeOfDay.ofHour(11))
        assertEquals(YearInReviewTimeOfDay.MIDDAY, YearInReviewTimeOfDay.ofHour(12))
        assertEquals(YearInReviewTimeOfDay.AFTERNOON, YearInReviewTimeOfDay.ofHour(13))
        assertEquals(YearInReviewTimeOfDay.AFTERNOON, YearInReviewTimeOfDay.ofHour(16))
        assertEquals(YearInReviewTimeOfDay.EVENING, YearInReviewTimeOfDay.ofHour(17))
        assertEquals(YearInReviewTimeOfDay.EVENING, YearInReviewTimeOfDay.ofHour(20))
        assertEquals(YearInReviewTimeOfDay.NIGHT, YearInReviewTimeOfDay.ofHour(21))
        assertEquals(YearInReviewTimeOfDay.NIGHT, YearInReviewTimeOfDay.ofHour(23))
        assertEquals(YearInReviewTimeOfDay.LATE_NIGHT, YearInReviewTimeOfDay.ofHour(0))
    }

    @Test
    fun `hours are summed into their time of day`() {
        val visitCountsByHour = listOf(
            HourVisitCount(hour = 12, visitCount = 3),
            HourVisitCount(hour = 13, visitCount = 2),
            HourVisitCount(hour = 15, visitCount = 2),
            HourVisitCount(hour = 22, visitCount = 1)
        )
        assertEquals(YearInReviewTimeOfDay.AFTERNOON to 50, YearInReviewTimeOfDay.favoriteOf(visitCountsByHour))
    }

    @Test
    fun `percentage is rounded`() {
        val visitCountsByHour = listOf(
            HourVisitCount(hour = 6, visitCount = 2),
            HourVisitCount(hour = 18, visitCount = 1)
        )
        assertEquals(YearInReviewTimeOfDay.MORNING to 67, YearInReviewTimeOfDay.favoriteOf(visitCountsByHour))
    }

    @Test
    fun `no favorite when no time of day has enough visits`() {
        val visitCountsByHour = listOf(
            HourVisitCount(hour = 6, visitCount = 1),
            HourVisitCount(hour = 18, visitCount = 1),
            HourVisitCount(hour = 23, visitCount = 1)
        )
        assertNull(YearInReviewTimeOfDay.favoriteOf(visitCountsByHour))
    }

    @Test
    fun `no favorite without any visits`() {
        assertNull(YearInReviewTimeOfDay.favoriteOf(emptyList()))
    }
}
