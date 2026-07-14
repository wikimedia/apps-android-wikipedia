package org.wikipedia.feed.wikigames

import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.WikiGames

// Represents all game types that can appear in the Explore feed and GamesHub.
// This can be extended whenever a new Wiki game is introduced.
sealed class WikiGame {
    // Stable identity of the game type, used for hide keys
    abstract val game: WikiGames

    data class OnThisDayGame(val state: OnThisDayCardGameState) : WikiGame() {
        override val game: WikiGames get() = WikiGames.WHICH_CAME_FIRST
    }
}

// UI state for Which Came First game.
// Preview --> state where user has not started today's game yet.
// InProgress --> state where user started but hasn't finished
// Completed --> state where user finished today's game and can see the results
sealed class OnThisDayCardGameState {
    abstract val langCode: String
    // The two events shown on the card are common to every state, so callers can read them without a when.
    abstract val event1: OnThisDay.Event
    abstract val event2: OnThisDay.Event

    data class Preview(
        override val langCode: String,
        override val event1: OnThisDay.Event,
        override val event2: OnThisDay.Event
    ) : OnThisDayCardGameState()

    data class InProgress(
        override val langCode: String,
        val currentQuestion: Int,
        override val event1: OnThisDay.Event = OnThisDay.Event(),
        override val event2: OnThisDay.Event = OnThisDay.Event()
    ) : OnThisDayCardGameState()

    data class Completed(
        override val langCode: String,
        val score: Int,
        val totalQuestions: Int,
        override val event1: OnThisDay.Event = OnThisDay.Event(),
        override val event2: OnThisDay.Event = OnThisDay.Event()
    ) : OnThisDayCardGameState()
}

// action for on this day game, future games events can also be created in similar way
sealed interface OnThisDayGameAction {
    data object Play : OnThisDayGameAction
    data object CountdownFinished : OnThisDayGameAction
    data object ReviewResults : OnThisDayGameAction
    data object PlayArchive : OnThisDayGameAction
}
