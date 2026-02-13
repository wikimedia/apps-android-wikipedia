package org.wikipedia.feed.wikigames

import org.wikipedia.feed.onthisday.OnThisDay

sealed class WikiGame {
    data class WhichCameFirst(val event1: OnThisDay.Event, val event2: OnThisDay.Event) : WikiGame()
    // TODO: remove this
    data class TestGame(val name: String) : WikiGame()
}
