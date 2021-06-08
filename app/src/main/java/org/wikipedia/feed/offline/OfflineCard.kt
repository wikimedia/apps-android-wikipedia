package org.wikipedia.feed.offline

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class OfflineCard : Card() {

    override fun type(): CardType {
        return CardType.OFFLINE
    }
}
