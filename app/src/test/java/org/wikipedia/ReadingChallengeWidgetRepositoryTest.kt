package org.wikipedia

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.wikipedia.settings.Prefs
import org.wikipedia.widgets.readingchallenge.ReadingChallengeState
import org.wikipedia.widgets.readingchallenge.ReadingChallengeUserData
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import java.time.LocalDate

class ReadingChallengeWidgetRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: ReadingChallengeWidgetRepository

    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        mockkObject(Prefs)
        repository = ReadingChallengeWidgetRepository(context)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // Not live yet tests
    @Test
    fun `returns NotLiveYet before May 1`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 4, 15),
                enabled = false,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.NotLiveYet)
    }

    @Test
    fun `does not return NotLiveYet on May 1`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 1),
                enabled = false,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertFalse(state is ReadingChallengeState.NotLiveYet)
    }

    // Not enrolled tests
    @Test
    fun `returns NotEnrolled on May 1 when not enrolled`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 1),
                enabled = false,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.NotEnrolled)
    }

    @Test
    fun `returns NotEnrolled on May 31 when not enrolled`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 31),
                enabled = false,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.NotEnrolled)
    }

    //  Challenge Completed (streak >= 25, enrolled, on or before July 10)
    @Test
    fun `returns ChallengeCompleted when streak is exactly 25 in May`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 25),
                enabled = true,
                currentStreak = 25,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `returns ChallengeCompleted when streak is 25 and date is June (after May but before July 10)`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 6, 15),
                enabled = true,
                currentStreak = 25,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `returns ChallengeCompleted on exactly July 10 with streak 25`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 7, 10),
                enabled = true,
                currentStreak = 25,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `does not return ChallengeCompleted on exactly July 10 with streak 24`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 7, 10),
                enabled = true,
                currentStreak = 24,
                hasReadToday = false
            )
        )
        assertFalse(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `does NOT return ChallengeCompleted when streak is 24`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 25),
                enabled = true,
                currentStreak = 24,
                hasReadToday = false
            )
        )
        assertFalse(state is ReadingChallengeState.ChallengeCompleted)
        // Should be either StreakOngoingNeedsReading or StreakOngoingReadToday
        assertTrue(
            state is ReadingChallengeState.StreakOngoingNeedsReading ||
                    state is ReadingChallengeState.StreakOngoingReadToday
        )
    }

    // Challenge Concluded tests
    @Test
    fun `returns ChallengeConcludedNoStreak on June 1 with zero streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 6, 1),
                enabled = true,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.ChallengeConcludedNoStreak)
    }

    @Test
    fun `returns ChallengeConcludedIncomplete on June 1 with partial streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 6, 1),
                enabled = true,
                currentStreak = 10,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.ChallengeConcludedIncomplete)
    }

    @Test
    fun `does NOT return Concluded states on May 31`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 31),
                enabled = true,
                currentStreak = 10,
                hasReadToday = false
            )
        )
        assertTrue(
            state is ReadingChallengeState.StreakOngoingNeedsReading ||
                    state is ReadingChallengeState.StreakOngoingReadToday
        )
    }

    // reset streak test
    @Test
    fun `when lastReadDate is more than 1 day ago then streak and hasReadToday are reset`() {
        val currentDate = LocalDate.of(2026, 3, 15)

        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(2).toString()

        var newCurrentStreak = 10

        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        assertEquals(0, newCurrentStreak)
    }

    // Enrolled not started test
    @Test
    fun `returns EnrolledNotStarted on May 1 with zero streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 1),
                enabled = true,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    @Test
    fun `returns EnrolledNotStarted mid-May with zero streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 15),
                enabled = true,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    // Streak Ongoing: Not Yet Read Today
    @Test
    fun `returns StreakOngoingNeedsReading when streak is 10 and not read today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 15),
                enabled = true,
                currentStreak = 10,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
    }

    @Test
    fun `StreakOngoingNeedsReading carries correct streak count`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 20),
                enabled = true,
                currentStreak = 5,
                hasReadToday = false
            )
        )
        assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
        assertEquals(5, (state as ReadingChallengeState.StreakOngoingNeedsReading).streak)
    }

    // Streak Ongoing: Already Read Today
    @Test
    fun `returns StreakOngoingReadToday when streak is 10 and read today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 15),
                enabled = true,
                currentStreak = 10,
                hasReadToday = true
            )
        )
        assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
    }

    @Test
    fun `StreakOngoingReadToday carries correct streak count`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 20),
                enabled = true,
                currentStreak = 7,
                hasReadToday = true
            )
        )
        assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
        assertEquals(7, (state as ReadingChallengeState.StreakOngoingReadToday).streak)
    }

    // Remove challenge test
    @Test
    fun `returns ChallengeRemoved on July 11`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 7, 11),
                enabled = true,
                currentStreak = 2,
                hasReadToday = false
            )
        )

        assertTrue(state is ReadingChallengeState.ChallengeRemoved)
    }
}
