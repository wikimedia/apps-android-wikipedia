package org.wikipedia.widgets.readingchallenge

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs

object ReadingChallengeAnalyticsHelper {
    val instrument: InstrumentImpl by lazy {
        // TODO: update instrument name after confirming with data analytics
        TestKitchenAdapter.client.getInstrument("apps-reading-challenge-widget")
            .startFunnel("widget_challenge")
    }

    // TODO: finalize the elementId
    fun logStreakOngoingNeedsReading() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_remind",
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }

    fun loadStreakOngoingReadToday() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_ongoing",
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }

    fun logChallengeConcluded() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_end",
            actionContext = mapOf("streak_count" to Prefs.readingChallengeStreak)
        )
    }

    fun logNotLiveYet() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_not_live_yet"
        )
    }

    fun logNotEnrolled() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_not_enrolled"
        )
    }

    fun logEnrolledNotStarted() {
        instrument.submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = "challenge_enrolled_not_started"
        )
    }
}
