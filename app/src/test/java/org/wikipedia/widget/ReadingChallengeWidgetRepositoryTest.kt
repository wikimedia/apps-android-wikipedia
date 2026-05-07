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
        every { Prefs.readingChallengeStartDate } returns START_DATE.toString()
        every { Prefs.readingChallengeEndDate } returns END_DATE.toString()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // Not live yet tests
    @Test
    fun `returns NotLiveYet before start date`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_BEFORE_START,
                enabled = false,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.NotLiveYet)
    }

    @Test
    fun `returns NotEnrolled on start date since challenge is Live`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = START_DATE,
                enabled = false,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.NotLiveYet)
        TestCase.assertTrue(state is ReadingChallengeState.NotEnrolled)
    }

    //  Challenge Completed (streak >= 25, enrolled, mid challenge)
    @Test
    fun `returns ChallengeCompleted when streak reaches goal in mid challenge`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE,
                enabled = true,
                currentStreak = 25,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `returns ChallengeCompleted when streak reaches goal near end of challenge`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = NEAR_END_CHALLENGE_DATE,
                enabled = true,
                currentStreak = 25,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `does NOT return ChallengeCompleted when streak is one below the goal`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE,
                enabled = true,
                currentStreak = READING_STREAK_GOAL - 1,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeCompleted)
        // Should be either StreakOngoingNeedsReading or StreakOngoingReadToday
        TestCase.assertTrue(
            state is ReadingChallengeState.StreakOngoingNeedsReading ||
                    state is ReadingChallengeState.StreakOngoingReadToday
        )
    }

    // END_DATE boundary behavior
    @Test
    fun `returns EnrolledNotStarted on END_DATE when streak is broken (allows fresh start)`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = END_DATE,
                enabled = true,
                currentStreak = 0, // already reset by recalculateStreakIfNeeded
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    @Test
    fun `return ChallengeConcludedIncomplete on day after END_DATE when streak is broken`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_END,
                enabled = true,
                currentStreak = 10,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedIncomplete)
        TestCase.assertEquals(10, (state as ReadingChallengeState.ChallengeConcludedIncomplete).streak)
    }

    @Test
    fun `does NOT return Concluded states on last day of challenge`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = END_DATE,
                enabled = true,
                currentStreak = 10,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeConcludedNoStreak)
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeConcludedIncomplete)
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeCompleted)
        TestCase.assertTrue(
            state is ReadingChallengeState.StreakOngoingNeedsReading ||
                    state is ReadingChallengeState.StreakOngoingReadToday
        )
    }

    @Test
    fun `returns ChallengeConcludedNoStreak when not enrolled past end date`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_END,
                enabled = false,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedNoStreak)
    }

    @Test
    fun `returns NotEnrolled on END_DATE itself when not enrolled (last chance to enroll)`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = END_DATE,
                enabled = false,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.NotEnrolled)
    }

    // reset streak test
    @Test
    fun `streak resets when lastReadDate is more than 1 day ago during challenge`() {
        val currentDate = MID_CHALLENGE_DATE

        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(2).toString()

        var newCurrentStreak = 10

        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(0, newCurrentStreak)
    }

    @Test
    fun `streak resets on exactly last day of challenge when lastReadDate is more than 1 day ago`() {
        val currentDate = END_DATE
        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(2).toString()

        var newCurrentStreak = 10
        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(0, newCurrentStreak)
    }

    @Test
    fun `streak should not reset after end date of when lastReadDate is more than 1 day ago`() {
        val currentDate = DAY_AFTER_END

        every { Prefs.readingChallengeEnrolled } returns true
        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(2).toString()
        var newCurrentStreak = 10

        // if this is called, it means the streak was reset, which should not happen after May 31
        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(10, newCurrentStreak)
    }

    @Test
    fun `streak does not reset when lastReadDate is exactly yesterday`() {
        val currentDate = MID_CHALLENGE_DATE

        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(1).toString()
        var newCurrentStreak = 10

        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(10, newCurrentStreak)
    }

    @Test
    fun `streak does not reset when lastReadDate is empty`() {
        val currentDate = MID_CHALLENGE_DATE

        every { Prefs.readingChallengeStreak } returns 10
        every { Prefs.readingChallengeLastReadDate } returns ""
        var newCurrentStreak = 10

        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(10, newCurrentStreak)
    }

    // Enrolled not started test
    @Test
    fun `returns EnrolledNotStarted on start date with zero streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = START_DATE,
                enabled = true,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.EnrolledNotStarted)
    }

    @Test
    fun `does NOT return EnrolledNotStarted after end date with zero streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_END,
                enabled = true,
                currentStreak = 0,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.EnrolledNotStarted)
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedNoStreak)
    }

    // Streak Ongoing: Not Yet Read Today
    @Test
    fun `returns StreakOngoingNeedsReading when streak is active and not read today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE,
                enabled = true,
                currentStreak = 10,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
    }

    @Test
    fun `StreakOngoingNeedsReading carries correct streak count`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE.plusDays(5),
                enabled = true,
                currentStreak = 5,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
        TestCase.assertEquals(5, (state as ReadingChallengeState.StreakOngoingNeedsReading).streak)
    }

    // Streak Ongoing: Already Read Today
    @Test
    fun `returns StreakOngoingReadToday when streak is active and read today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE.plusDays(2),
                enabled = true,
                currentStreak = 10,
                hasReadToday = true,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
    }

    @Test
    fun `StreakOngoingReadToday carries correct streak count`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = MID_CHALLENGE_DATE.plusDays(6),
                enabled = true,
                currentStreak = 7,
                hasReadToday = true,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
        TestCase.assertEquals(7, (state as ReadingChallengeState.StreakOngoingReadToday).streak)
    }

    // Remove challenge test
    @Test
    fun `returns ChallengeRemoved after remove date`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_REMOVE,
                enabled = true,
                currentStreak = 2,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )

        TestCase.assertTrue(state is ReadingChallengeState.ChallengeRemoved)
    }

    // has read today test
    @Test
    fun `hasReadToday returns true when lastReadDate matches currentDate`() {
        val currentDate = MID_CHALLENGE_DATE
        every { Prefs.readingChallengeLastReadDate } returns currentDate.toString()

        TestCase.assertTrue(repository.hasReadToday(currentDate))
    }

    @Test
    fun `hasReadToday returns false when lastReadDate is yesterday`() {
        val currentDate = MID_CHALLENGE_DATE
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(1).toString()

        TestCase.assertFalse(repository.hasReadToday(currentDate))
    }

    @Test
    fun `hasReadToday returns false when lastReadDate is empty`() {
        val currentDate = MID_CHALLENGE_DATE
        every { Prefs.readingChallengeLastReadDate } returns ""

        TestCase.assertFalse(repository.hasReadToday(currentDate))
    }

    // Active streak past end date
    @Test
    fun `returns StreakOngoingNeedsReading after end date when streak is active and not read today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_END,
                enabled = true,
                currentStreak = 12,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
        TestCase.assertEquals(12, (state as ReadingChallengeState.StreakOngoingNeedsReading).streak)
    }

    @Test
    fun `returns StreakOngoingReadToday after end date when streak is active and ready today`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = DAY_AFTER_END,
                enabled = true,
                currentStreak = 12,
                hasReadToday = true,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingReadToday)
        TestCase.assertEquals(12, (state as ReadingChallengeState.StreakOngoingReadToday).streak)
    }

    @Test
    fun `returns StreakOngoingNeedsReading well past end date when streak is still active`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = AFTER_END_BEFORE_REMOVE_DATE,
                enabled = true,
                currentStreak = 20,
                hasReadToday = false,
                hasActiveStreak = true
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.StreakOngoingNeedsReading)
        TestCase.assertEquals(20, (state as ReadingChallengeState.StreakOngoingNeedsReading).streak)
    }

    // Streak broken past end date
    @Test
    fun `ChallengeCompleted is terminal even past end date with broken streak`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = AFTER_END_BEFORE_REMOVE_DATE,
                enabled = true,
                currentStreak = 25,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeCompleted)
    }

    @Test
    fun `does NOT return ChallengeRemoved on REMOVE_DATE itself`() {
        val state = repository.resolveState(
            ReadingChallengeUserData(
                currentDate = REMOVE_DATE,
                enabled = true,
                currentStreak = 10,
                hasReadToday = false,
                hasActiveStreak = false
            )
        )
        TestCase.assertFalse(state is ReadingChallengeState.ChallengeRemoved)
        TestCase.assertTrue(state is ReadingChallengeState.ChallengeConcludedIncomplete)
    }

    @Test
    fun `streak does not reset after completion even if user stops reading`() {
        val currentDate = MID_CHALLENGE_DATE

        every { Prefs.readingChallengeStreak } returns 25
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(3).toString()

        var newCurrentStreak = 25
        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)
        TestCase.assertEquals(25, newCurrentStreak) // completed, must not reset
    }

    @Test
    fun `streak does not reset after completion when streak exceeds goal`() {
        val currentDate = MID_CHALLENGE_DATE

        every { Prefs.readingChallengeStreak } returns 30
        every { Prefs.readingChallengeLastReadDate } returns currentDate.minusDays(5).toString()

        var newCurrentStreak = 30
        every { Prefs.readingChallengeStreak = any() } answers { newCurrentStreak = firstArg() }

        repository.recalculateStreakIfNeeded(currentDate)

        TestCase.assertEquals(30, newCurrentStreak)
    }

    companion object {
        // Update only these when dates change
        private val START_DATE = LocalDate.of(2026, 5, 11)
        private val END_DATE = LocalDate.of(2026, 6, 18)
        private val REMOVE_DATE = LocalDate.of(2026, 7, 27)

        private const val READING_STREAK_GOAL = 25

        // Derive dates based on the above dates
        private val DAY_BEFORE_START = START_DATE.minusDays(1)
        private val DAY_AFTER_END = END_DATE.plusDays(1)
        private val DAY_AFTER_REMOVE = REMOVE_DATE.plusDays(1)
        private val MID_CHALLENGE_DATE = START_DATE.plusDays(15)
        private val NEAR_END_CHALLENGE_DATE = END_DATE.minusDays(3)
        private val AFTER_END_BEFORE_REMOVE_DATE = REMOVE_DATE.minusDays(5)
    }
}
