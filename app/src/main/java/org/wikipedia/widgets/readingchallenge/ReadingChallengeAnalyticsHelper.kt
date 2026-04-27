package org.wikipedia.widgets.readingchallenge

import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs

object ReadingChallengeAnalyticsHelper {
    private val instrument = TestKitchenAdapter.client.getInstrument("apps-widgetchallenge")

    fun sendHeartbeatEvent(state: ReadingChallengeState) {
        when (state) {
            ReadingChallengeState.ChallengeCompleted -> logHeartbeatWithStreak(elementId = "challenge_completed")
            is ReadingChallengeState.ChallengeConcludedIncomplete -> logHeartbeatWithStreak(elementId = "challenge_incomplete")
            ReadingChallengeState.ChallengeConcludedNoStreak -> logHeartbeatWithStreak(elementId = "challenge_no_streak")
            is ReadingChallengeState.StreakOngoingNeedsReading -> logHeartbeatWithStreak(elementId = "streak_ongoing")
            is ReadingChallengeState.StreakOngoingReadToday -> logHeartbeatWithStreak(elementId = "streak_ongoing_read")
            ReadingChallengeState.NotEnrolled -> logHeartbeatWithoutStreak(elementId = "not_enrolled")
            ReadingChallengeState.NotLiveYet -> logHeartbeatWithoutStreak(elementId = "not_yet_live")
            ReadingChallengeState.EnrolledNotStarted -> logHeartbeatWithoutStreak(elementId = "enrolled_not_started")
            else -> {}
        }
    }

    fun logAppOpenFromWidget() {
        TestKitchenAdapter.client.getInstrument("apps-open")
            .submitInteraction(
                action = "app_open",
                actionSource = "widget",
                actionSubtype = "reading_challenge"
            )
    }

    private fun logHeartbeatWithoutStreak(elementId: String) {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = elementId,
        )
    }

    private fun logHeartbeatWithStreak(elementId: String) {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = elementId,
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }
}
