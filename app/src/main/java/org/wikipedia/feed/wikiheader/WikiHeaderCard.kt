package org.wikipedia.feed.wikiheader

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class WikiHeaderCard : Card() {
    override fun type(): CardType {
        return CardType.WIKI_HEADER
    }
}