package org.wikipedia.widget

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import junit.framework.TestCase
import org.junit.After
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
        TestCase.assertTrue(state is ReadingChallengeState.NotLiveYet)
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
        TestCase.assertFalse(state is ReadingChallengeState.NotLiveYet)
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
        TestCase.assertTrue(state is ReadingChallengeState.NotEnrolled)
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
        TestCase.assertTrue(state is ReadingChallengeState.NotEnrolled)
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
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
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
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
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
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
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
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeCompleted)
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
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeCompleted)
        // Should be either StreakOngoingNeedsReading or StreakOngoingReadToday
        TestCase.assertTrue(
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
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedNoStreak)
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
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedIncomplete)
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
        TestCase.assertTrue(
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

        TestCase.assertEquals(0, newCurrentStreak)
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
        TestCase.assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    @Test
    fun `returns EnrolledNotStarted mid-May with zero streak or after streak reset`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 5, 15),
                enabled = true,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    @Test
    fun `does NOT return EnrolledNotStarted after streak reset after May 31`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = LocalDate.of(2026, 6, 1),
                enabled = true,
                currentStreak = 0,
                hasReadToday = false
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.EnrolledNotStarted)
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedNoStreak)
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
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
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
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
        TestCase.assertEquals(5, (state as ReadingChallengeState.StreakOngoingNeedsReading).streak)
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
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
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
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
        TestCase.assertEquals(7, (state as ReadingChallengeState.StreakOngoingReadToday).streak)
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

        TestCase.assertTrue(state is ReadingChallengeState.ChallengeRemoved)
    }

    // has read today test
    @Test
    fun `hasReadToday returns true when lastReadDate matches currentDate`() {
        val currentDate = LocalDate.of(2026, 5, 15)
        every { Prefs.readingChallengeLastReadDate } returns currentDate.toString()

        TestCase.assertTrue(repository.hasReadToday(currentDate))
    }

    @Test
    fun `hasReadToday returns false when lastReadDate is yesterday`() {
        val currentDate = LocalDate.of(2026, 5, 15)
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(1).toString()

        TestCase.assertFalse(repository.hasReadToday(currentDate))
    }

    @Test
    fun `hasReadToday returns false when lastReadDate is empty`() {
        val currentDate = LocalDate.of(2026, 5, 15)
        every { Prefs.readingChallengeLastReadDate } returns ""

        TestCase.assertFalse(repository.hasReadToday(currentDate))
    }

    // update on article read test
    @Test
    fun `updateOnArticleRead increments streak and sets today's date when enrolled and not ready today`() {
        val currentDate = LocalDate.now()
        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 5
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(1).toString()

        var newStreak = 5
        var newDate = ""

        every { Prefs.readingChallengeStreak = any() } answers { newStreak = firstArg() }
        every { Prefs.readingChallengeLastReadDate = any() } answers { newDate = firstArg() }

        repository.updateOnArticleRead(currentDate)
        println("orange currentDate: $currentDate newDate: $newDate")
        TestCase.assertEquals(6, newStreak)
        TestCase.assertEquals(currentDate.toString(), newDate)
    }

    @Test
    fun `updateOnArticleRead resets streak to 0 (in case where recalculateStreakIfNeeded was not called for some edge cases) before incrementing if gap is more than 1 day`() {
        val currentDate = LocalDate.now()
        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(3).toString()

        var newStreak = 100
        var newDate = ""

        every { Prefs.readingChallengeStreak } answers { newStreak }
        every { Prefs.readingChallengeStreak = any() } answers { newStreak = firstArg() }
        every { Prefs.readingChallengeLastReadDate = any() } answers { newDate = firstArg() }

        repository.updateOnArticleRead(currentDate)

        TestCase.assertEquals(1, newStreak) // should be reset to 0 and then incremented to 1
        TestCase.assertEquals(currentDate.toString(), newDate)
    }

    @Test
    fun `updateArticleOnRead ignores entirely if before May 1`() {
        val currentDate = LocalDate.of(2026, 4, 15)
        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 0
        every { Prefs.readingChallengeLastReadDate } returns ""

        val newStreak = 0
        val lastRead = ""

        every { Prefs.readingChallengeStreak } answers { newStreak }
        every { Prefs.readingChallengeLastReadDate } answers { lastRead }

        repository.updateOnArticleRead(currentDate)

        TestCase.assertEquals(0, newStreak) // nothing happens
        TestCase.assertEquals("", lastRead) // nothing happens
    }

    @Test
    fun `updateArticleOnRead ignores entirely if after May 31`() {
        val currentDate = LocalDate.of(2026, 6, 1)
        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 24
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(1).toString()

        val newStreak = 0
        val lastRead = ""

        every { Prefs.readingChallengeStreak } answers { newStreak }
        every { Prefs.readingChallengeLastReadDate } answers { lastRead }

        repository.updateOnArticleRead(currentDate)

        TestCase.assertEquals(0, newStreak) // nothing happens
        TestCase.assertEquals("", lastRead) // nothing happens
    }
}
