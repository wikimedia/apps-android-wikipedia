package org.wikipedia.feed.progress

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class ProgressCard : Card() {

    override fun type(): CardType {
        return CardType.PROGRESS
    }
}
