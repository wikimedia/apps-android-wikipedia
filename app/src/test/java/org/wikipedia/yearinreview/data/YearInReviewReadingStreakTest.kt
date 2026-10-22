package org.wikipedia.yearinreview.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class YearInReviewReadingStreakTest {

    @Test
    fun `no streak without any days`() {
        assertNull(YearInReviewReadingStreak.longestOf(emptyList()))
    }

    @Test
    fun `a single day is a one day streak`() {
        val streak = YearInReviewReadingStreak.longestOf(listOf(LocalDate.of(2026, 3, 4)))
        assertEquals(YearInReviewReadingStreak(start = LocalDate.of(2026, 3, 4), end = LocalDate.of(2026, 3, 4)), streak)
        assertEquals(1, streak?.days)
    }

    @Test
    fun `the longest run of consecutive days wins`() {
        val days = listOf(
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 1, 11),
            LocalDate.of(2026, 3, 4),
            LocalDate.of(2026, 3, 5),
            LocalDate.of(2026, 3, 6),
            LocalDate.of(2026, 3, 8)
        )
        val streak = YearInReviewReadingStreak.longestOf(days)
        assertEquals(YearInReviewReadingStreak(start = LocalDate.of(2026, 3, 4), end = LocalDate.of(2026, 3, 6)), streak)
        assertEquals(3, streak?.days)
    }

    @Test
    fun `a tie goes to the more recent streak`() {
        val days = listOf(
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 1, 11),
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 21)
        )
        assertEquals(YearInReviewReadingStreak(start = LocalDate.of(2026, 5, 20), end = LocalDate.of(2026, 5, 21)), YearInReviewReadingStreak.longestOf(days))
    }

    @Test
    fun `a streak continues across months and years`() {
        val days = listOf(
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 2)
        )
        val streak = YearInReviewReadingStreak.longestOf(days)
        assertEquals(YearInReviewReadingStreak(start = LocalDate.of(2026, 1, 31), end = LocalDate.of(2026, 2, 2)), streak)
        assertEquals(3, streak?.days)
    }
}
