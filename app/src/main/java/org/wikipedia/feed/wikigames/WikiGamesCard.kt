package org.wikipedia.feed.wikigames

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class WikiGamesCard(val wikiSite: WikiSite) : Card() {
    override fun type(): CardType {
        return CardType.WIKI_GAMES
    }
}
