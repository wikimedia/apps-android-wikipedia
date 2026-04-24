package org.wikipedia.widgets.readingchallenge

import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs

object ReadingChallengeAnalyticsHelper {
    // TODO: waiting on decision on whether to remove funnel or not for hearbeat events
    fun getInstrumentation() = TestKitchenAdapter.client.getInstrument("apps-widgetchallenge")
        .startFunnel("widget_challenge")

    fun sendHeartbeatEvent(state: ReadingChallengeState) {
        when (state) {
            ReadingChallengeState.ChallengeCompleted -> logChallengeConcluded(elementId = "challenge_completed")
            is ReadingChallengeState.ChallengeConcludedIncomplete -> logChallengeConcluded(elementId = "challenge_incomplete")
            ReadingChallengeState.ChallengeConcludedNoStreak -> logChallengeConcluded(elementId = "challenge_no_streak")
            is ReadingChallengeState.StreakOngoingNeedsReading,
            is ReadingChallengeState.StreakOngoingReadToday -> {
                getInstrumentation().submitInteraction(
                    action = "heartbeat",
                    actionSource = "widget_challenge",
                    actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
                )
            }
            ReadingChallengeState.NotEnrolled,
            ReadingChallengeState.NotLiveYet,
            ReadingChallengeState.EnrolledNotStarted -> {
                getInstrumentation().submitInteraction(
                    action = "heartbeat",
                    actionSource = "widget_challenge"
                )
            }
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

    private fun logChallengeConcluded(elementId: String) {
        getInstrumentation().submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = elementId,
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }
}
