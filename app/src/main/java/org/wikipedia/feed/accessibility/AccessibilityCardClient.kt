package org.wikipedia.feed.accessibility

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card

class AccessibilityCardClient : DummyClient() {

    override fun getNewCard(wiki: WikiSite?): Card {
        return AccessibilityCard()
    }
}
