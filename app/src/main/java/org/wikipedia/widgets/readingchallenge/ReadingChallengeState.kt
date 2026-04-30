package org.wikipedia.widgets.readingchallenge

import java.time.LocalDate

sealed interface ReadingChallengeState {
    object Loading : ReadingChallengeState
    // State 1: Pre-enrollment before May 1, 2026
    object NotLiveYet : ReadingChallengeState

    // State 2: Challenge Active (May 1 to May 31, 2026)

    // User has not joined
    object NotEnrolled : ReadingChallengeState

    // Joined && article not opened
    object EnrolledNotStarted : ReadingChallengeState

    // Joined && streak of 1+ days && no article opened
    data class StreakOngoingNeedsReading(val streak: Int) : ReadingChallengeState

    // Joined && streak of 1+ days && article opened
    data class StreakOngoingReadToday(val streak: Int) : ReadingChallengeState

    // State 3: Success
    object ChallengeCompleted : ReadingChallengeState

    // State 4: Post Challenge (After May 31, 2026)
    data class ChallengeConcludedIncomplete(val streak: Int) : ReadingChallengeState // Joined && did not dit 25  streak

    object ChallengeConcludedNoStreak : ReadingChallengeState // Joined && 0 streak

    // State 5: Challenge Remove
    object ChallengeRemoved : ReadingChallengeState
}

data class ReadingChallengeUserData(
    val currentDate: LocalDate,
    val enabled: Boolean,
    val currentStreak: Int,
    val hasReadToday: Boolean,
    val hasActiveStreak: Boolean
)
