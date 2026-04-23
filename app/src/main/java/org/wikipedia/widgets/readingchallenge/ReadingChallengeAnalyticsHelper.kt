package org.wikipedia.widgets.readingchallenge

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs

object ReadingChallengeAnalyticsHelper {
    val instrument: InstrumentImpl by lazy {
        TestKitchenAdapter.client.getInstrument("apps-widgetchallenge")
            .startFunnel("widget_challenge")
    }

    fun sendAnalytics(state: ReadingChallengeState) {
        when (state) {
            ReadingChallengeState.ChallengeCompleted -> logChallengeConcluded(elementId = "challenge_completed")
            is ReadingChallengeState.ChallengeConcludedIncomplete -> logChallengeConcluded(elementId = "challenge_incomplete")
            ReadingChallengeState.ChallengeConcludedNoStreak -> logChallengeConcluded(elementId = "challenge_no_streak")
            is ReadingChallengeState.StreakOngoingNeedsReading,
            is ReadingChallengeState.StreakOngoingReadToday -> {
                instrument.submitInteraction(
                    action = "heartbeat",
                    actionSource = "widget_challenge",
                    actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
                )
            }
            ReadingChallengeState.NotEnrolled,
            ReadingChallengeState.NotLiveYet,
            ReadingChallengeState.EnrolledNotStarted -> {
                instrument.submitInteraction(
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
                actionSource = "widget"
            )
    }

    private fun logChallengeConcluded(elementId: String) {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = elementId,
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }
}
