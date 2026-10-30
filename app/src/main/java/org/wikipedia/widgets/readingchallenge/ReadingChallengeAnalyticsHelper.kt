package org.wikipedia.widgets.readingchallenge

import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs

object ReadingChallengeAnalyticsHelper {
    fun sendHeartbeatEvent(state: ReadingChallengeState) {
        when (state) {
            ReadingChallengeState.ChallengeCompleted -> logHeartbeat(elementId = "challenge_completed", includeStreak = true)
            is ReadingChallengeState.ChallengeConcludedIncomplete -> logHeartbeat(elementId = "challenge_incomplete", includeStreak = true)
            ReadingChallengeState.ChallengeConcludedNoStreak -> logHeartbeat(elementId = "challenge_no_streak", includeStreak = true)
            is ReadingChallengeState.StreakOngoingNeedsReading -> logHeartbeat(elementId = "streak_ongoing", includeStreak = true)
            is ReadingChallengeState.StreakOngoingReadToday -> logHeartbeat(elementId = "streak_ongoing_read", includeStreak = true)
            ReadingChallengeState.NotEnrolled -> logHeartbeat(elementId = "not_enrolled")
            ReadingChallengeState.NotLiveYet -> logHeartbeat(elementId = "not_yet_live")
            ReadingChallengeState.EnrolledNotStarted -> logHeartbeat(elementId = "enrolled_not_started")
            ReadingChallengeState.ChallengeRemoved -> logHeartbeat(elementId = "challenge_removed", includeStreak = true)
            ReadingChallengeState.RandomArticle -> logRandomArticleHeartbeat()
            else -> {}
        }
    }

    private fun logHeartbeat(elementId: String, includeStreak: Boolean = false) {
        TestKitchenAdapter.client.getInstrument("apps-widgetchallenge").submitInteraction(
            action = "heartbeat",
            actionSource = "widget_challenge",
            elementId = elementId,
            actionContext = if (includeStreak) mapOf("streak_count" to Prefs.readingChallengeStreak) else null
        )
    }

    private fun logRandomArticleHeartbeat() {
        TestKitchenAdapter.client.getInstrument("apps-widget").submitInteraction(
            action = "heartbeat",
            actionSource = "widget_random_article"
        )
    }
}
