package org.wikipedia.feed.wikigames

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.onthisday.OnThisDay

class WikiGamesCard(val wikiSite: WikiSite, val event1: OnThisDay.Event, val event2: OnThisDay.Event) : Card() {
    override fun type(): CardType {
        return CardType.WIKI_GAMES
    }
}
