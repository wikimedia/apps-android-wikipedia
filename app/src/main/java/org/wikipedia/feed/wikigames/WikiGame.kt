package org.wikipedia.feed.wikigames

import org.wikipedia.feed.onthisday.OnThisDay

// Represents all game types that can appear in the Explore feed and GamesHub.
// This can be extended whenever a new Wiki game is introduced.
sealed class WikiGame {
    data class OnThisDayGame(val state: OnThisDayCardGameState) : WikiGame()
}

// UI state for Which Came First game.
// Preview --> state where user has not started today's game yet.
// InProgress --> state where user started but hasn't finished
// Completed --> state where user finished today's game and can see the results
sealed class OnThisDayCardGameState {
    data class Preview(val langCode: String, val event1: OnThisDay.Event, val event2: OnThisDay.Event) : OnThisDayCardGameState()
    data class InProgress(val langCode: String, val currentQuestion: Int) : OnThisDayCardGameState()
    data class Completed(val langCode: String, val score: Int, val totalQuestions: Int) : OnThisDayCardGameState()
}
