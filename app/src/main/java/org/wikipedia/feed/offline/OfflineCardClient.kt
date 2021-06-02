package org.wikipedia.feed.offline

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card

class OfflineCardClient : DummyClient() {
    override fun getNewCard(wiki: WikiSite?): Card {
        return OfflineCard()
    }
}
