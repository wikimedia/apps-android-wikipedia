package org.wikipedia.widgets.readingchallenge

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.glance.appwidget.updateAll
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReadingChallengeWidgetRepository(private val context: Context) {
    fun observeState(): Flow<ReadingChallengeState> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val relevantKeys = setOf(
            context.getString(R.string.preference_key_reading_challenge_streak),
            context.getString(R.string.preference_key_reading_challenge_enrolled),
            context.getString(R.string.preference_key_reading_challenge_last_read_date)
        )
        return callbackFlow {
            trySend(ReadingChallengeState.Loading) // initial loading state
            fun emit() {
                trySend(getCurrentState())
            }

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key in relevantKeys) {
                    emit()
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            emit() // for daily updates and to emit initial value when flow is collected
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }.distinctUntilChanged()
    }

    fun hasReadToday(currentDate: LocalDate): Boolean {
        Prefs.readingChallengeLastReadDate.let {
            return if (it.isNotEmpty()) {
                LocalDate.parse(it) == currentDate
            } else {
                false
            }
        }
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
        if (!userData.enabled) {
            return ReadingChallengeState.NotEnrolled
        }

        // From this point onward, user is enrolled

        // Stage 3: Success State, once user is enrolled we check immediately to bypass other conditions if they finish on time
        if (userData.currentStreak >= READING_STREAK_GOAL) {
            return ReadingChallengeState.ChallengeCompleted
        }

        // Stage 4: Post Challenge (After May 31, 2026)
        if (userData.currentDate.isAfter(END_DATE)) {
            return if (userData.currentStreak > 0) {
                ReadingChallengeState.ChallengeConcludedIncomplete(userData.currentStreak) // streak did not hit 25, but they did have a streak
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

    private fun getCurrentState(currentDate: LocalDate = LocalDate.now()): ReadingChallengeState {
        recalculateStreakIfNeeded(currentDate)
        return resolveState(
            ReadingChallengeUserData(
                currentDate = currentDate,
                enabled = Prefs.readingChallengeEnrolled,
                currentStreak = Prefs.readingChallengeStreak,
                hasReadToday = hasReadToday(currentDate)
            )
        )
    }

    fun recalculateStreakIfNeeded(currentDate: LocalDate) {
        if (currentDate.isAfter(END_DATE)) return // will not reset after challenge ends

        val lastReadDateStr = Prefs.readingChallengeLastReadDate
        if (lastReadDateStr.isNotEmpty()) {
            val lastReadDate = LocalDate.parse(lastReadDateStr)
            val daysBetween = ChronoUnit.DAYS.between(lastReadDate, currentDate)
            if (daysBetween > 1) {
                Prefs.readingChallengeStreak = 0
            }
        }
    }

    suspend fun updateWidgetsAndSendAnalytics() {
        ReadingChallengeAnalyticsHelper.sendHeartbeatEvent(getCurrentState())
        ReadingChallengeWidget().updateAll(context)
    }
    suspend fun updateOnArticleRead(currentDate: LocalDate) {
        if (currentDate.isBefore(START_DATE) || currentDate.isAfter(END_DATE)) {
            return
        }

        if (Prefs.readingChallengeEnrolled && !hasReadToday(currentDate)) {
            Prefs.readingChallengeLastReadDate = currentDate.toString()
            Prefs.readingChallengeStreak += 1
            updateWidgetsAndSendAnalytics()
        }
    }

    companion object {
        const val READING_STREAK_GOAL = 25
        const val INTENT_EXTRA_READING_CHALLENGE_REWARD = "reading_challenge_reward"
        const val READING_CHALLENGE_END_DATE = "2026-06-18"
        const val READING_CHALLENGE_START_DATE = "2026-05-11"
        val START_DATE get() = LocalDate.parse(Prefs.readingChallengeStartDate.ifEmpty { READING_CHALLENGE_START_DATE })
        val END_DATE get() = LocalDate.parse(Prefs.readingChallengeEndDate.ifEmpty { READING_CHALLENGE_END_DATE })
        private val REMOVE_DATE = LocalDate.of(2026, 7, 27)

        private val isChallengeActive: Boolean
            get() = !LocalDate.now().isBefore(START_DATE) && LocalDate.now().isBefore(END_DATE)

        fun isWidgetInstalled(): Boolean {
            val context = WikipediaApp.instance
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, ReadingChallengeWidgetReceiver::class.java)
            )
            return ids.isNotEmpty()
        }

        fun shouldShowOnboardingDialog(): Boolean {
            return !Prefs.readingChallengeOnboardingShown && isChallengeActive
        }

        fun shouldShowWidgetInstallDialog(): Boolean {
            return Prefs.readingChallengeOnboardingShown && !Prefs.readingChallengeInstallPromptShown &&
                    Prefs.readingChallengeEnrolled && AccountUtil.isLoggedIn && isChallengeActive && !isWidgetInstalled()
        }

        fun shouldShowReward(intent: Intent): Boolean {
            return intent.hasExtra(INTENT_EXTRA_READING_CHALLENGE_REWARD) && AccountUtil.isLoggedIn
        }
    }
}
