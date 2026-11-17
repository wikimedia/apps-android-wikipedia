package org.wikipedia.recurring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import java.time.LocalDate

class RecommendedReadingListTaskTest {

    @Test
    fun testDailyNotDueWhenAlreadyUpdatedToday() {
        assertFalse(isUpdateDue(lastRun = "2026-11-04", today = "2026-11-04", RecommendedReadingListUpdateFrequency.DAILY))
    }

    @Test
    fun testDailyDueOnNextDay() {
        assertTrue(isUpdateDue(lastRun = "2026-11-03", today = "2026-11-04", RecommendedReadingListUpdateFrequency.DAILY))
    }

    @Test
    fun testDailyDueAfterMissedDays() {
        assertTrue(isUpdateDue(lastRun = "2026-10-28", today = "2026-11-04", RecommendedReadingListUpdateFrequency.DAILY))
    }

    @Test
    fun testWeeklyNotDueBeforeNextMonday() {
        // Last updated on Monday, Nov 2; today is Sunday, Nov 8.
        assertFalse(isUpdateDue(lastRun = "2026-11-02", today = "2026-11-08", RecommendedReadingListUpdateFrequency.WEEKLY))
    }

    @Test
    fun testWeeklyNotDueWhenUpdatedMidWeek() {
        // Last updated on Wednesday, Nov 4; today is Friday, Nov 6.
        assertFalse(isUpdateDue(lastRun = "2026-11-04", today = "2026-11-06", RecommendedReadingListUpdateFrequency.WEEKLY))
    }

    @Test
    fun testWeeklyDueOnMonday() {
        assertTrue(isUpdateDue(lastRun = "2026-11-06", today = "2026-11-09", RecommendedReadingListUpdateFrequency.WEEKLY))
    }

    @Test
    fun testWeeklyDueWhenMondayWasMissed() {
        // Last updated on Friday, Nov 6; the app wasn't opened on Monday, Nov 9, and today is Wednesday, Nov 11.
        assertTrue(isUpdateDue(lastRun = "2026-11-06", today = "2026-11-11", RecommendedReadingListUpdateFrequency.WEEKLY))
    }

    @Test
    fun testMonthlyNotDueWithinSameMonth() {
        assertFalse(isUpdateDue(lastRun = "2026-11-01", today = "2026-11-30", RecommendedReadingListUpdateFrequency.MONTHLY))
    }

    @Test
    fun testMonthlyDueOnFirstDayOfMonth() {
        assertTrue(isUpdateDue(lastRun = "2026-10-15", today = "2026-11-01", RecommendedReadingListUpdateFrequency.MONTHLY))
    }

    @Test
    fun testMonthlyDueWhenFirstDayWasMissed() {
        assertTrue(isUpdateDue(lastRun = "2026-10-15", today = "2026-11-03", RecommendedReadingListUpdateFrequency.MONTHLY))
    }

    @Test
    fun testMonthlyDueAcrossYearBoundary() {
        assertTrue(isUpdateDue(lastRun = "2026-12-20", today = "2027-01-05", RecommendedReadingListUpdateFrequency.MONTHLY))
    }

    private fun isUpdateDue(lastRun: String, today: String, frequency: RecommendedReadingListUpdateFrequency): Boolean {
        return RecommendedReadingListTask.isUpdateDue(LocalDate.parse(lastRun), LocalDate.parse(today), frequency)
    }
}
