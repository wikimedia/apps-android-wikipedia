package org.wikipedia.feed.wikigames

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class WikiGamesCard : Card() {
    override fun type(): CardType {
        return CardType.WIKI_GAMES
    }
}
