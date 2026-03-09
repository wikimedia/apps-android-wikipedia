package org.wikipedia.widgets.readingchallenge

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wikipedia.settings.Prefs
import java.time.LocalDate

class ReadingChallengeWidgetRepository(private val context: Context) {
    // TODO: restore to actual start and end date
    private val START_DATE = LocalDate.of(2026, 3, 1)
    private val END_DATE = LocalDate.of(2026, 3, 31)
    private val REMOVE_DATE = LocalDate.of(2026, 7, 10)

    fun observeState(): Flow<ReadingChallengeState> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return callbackFlow {
            fun emit() {
                val currentDate = LocalDate.now()
                recalculateStreakIfNeeded(currentDate)
                trySend(resolveState(
                    ReadingChallengeUserData(
                        currentDate = currentDate,
                        isEnrolled = Prefs.isEnrolled,
                        currentStreak = Prefs.currentStreak,
                        hasReadToday = Prefs.hasReadToday
                    )
                ))
            }

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> emit() }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            emit() // for daily updates and to emit initial value when flow is collected
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }.distinctUntilChanged()
    }

    fun resolveState(userData: ReadingChallengeUserData): ReadingChallengeState {
        // Stage 5: Remove challenge
        if (userData.currentDate.isAfter(REMOVE_DATE)) {
            return ReadingChallengeState.ChallengeRemoved
        }

        // Stage 1: Pre-enrollment
        if (userData.currentDate.isBefore(START_DATE)) {
            return ReadingChallengeState.NotLiveYet
        }

        // From this point onward, we are in the active or past the challenge period

        // Stage 2: Challenge is active and user has not enrolled
        if (!userData.isEnrolled) {
            return ReadingChallengeState.NotEnrolled
        }

        // From this point onward, user is enrolled

        // Stage 3: Success State, once user is enrolled we check immediately to bypass other conditions if they finish on time
        if (userData.currentStreak >= 25) {
            return ReadingChallengeState.ChallengeCompleted
        }

        // Stage 4: Post Challenge (After May 31, 2026)
        if (userData.currentDate.isAfter(END_DATE)) {
            return if (userData.currentStreak > 0) {
                ReadingChallengeState.ChallengeConcludedIncomplete // streak did not hit 25, but they did have a streak
            } else {
                ReadingChallengeState.ChallengeConcludedNoStreak // no streak at all
            }
        }

        // Stage 2: no article opened
        if (userData.currentStreak == 0) {
            return ReadingChallengeState.EnrolledNotStarted
        }

        // Stage 2: Active streak
        return if (userData.hasReadToday) {
            ReadingChallengeState.StreakOngoingReadToday(userData.currentStreak)
        } else {
            ReadingChallengeState.StreakOngoingNeedsReading(userData.currentStreak)
        }
    }

    fun recalculateStreakIfNeeded(currentDate: LocalDate) {
        val lastReadDateStr = Prefs.lastReadDate
        if (lastReadDateStr.isNotEmpty()) {
            val lastReadDate = LocalDate.parse(lastReadDateStr)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastReadDate,
                currentDate)
            if (daysBetween > 1) {
                Prefs.hasReadToday = false
                Prefs.currentStreak = 0
            }
        }
    }
}
