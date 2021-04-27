package org.wikipedia.feed.searchbar

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class SearchCard : Card() {
    override fun type(): CardType {
        return CardType.SEARCH_BAR
    }
}
