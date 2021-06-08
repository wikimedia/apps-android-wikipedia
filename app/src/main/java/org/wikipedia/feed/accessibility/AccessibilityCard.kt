package org.wikipedia.feed.accessibility

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

class AccessibilityCard : Card() {
    override fun type(): CardType {
        return CardType.ACCESSIBILITY
    }
}
