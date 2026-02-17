package org.wikipedia.feed.wikigames

import org.wikipedia.feed.onthisday.OnThisDay

sealed class WikiGame {
    data class OnThisDayGame(val state: OnThisDayCardGameState) : WikiGame()
}

sealed class OnThisDayCardGameState {
    data class Preview(val event1: OnThisDay.Event, val event2: OnThisDay.Event) : OnThisDayCardGameState()
    data class InProgress(val currentQuestion: Int) : OnThisDayCardGameState()
    data class Completed(val score: Int, val totalQuestion: Int) : OnThisDayCardGameState()
}
