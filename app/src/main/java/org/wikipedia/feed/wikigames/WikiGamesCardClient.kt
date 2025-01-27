package org.wikipedia.feed.wikigames

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card

class WikiGamesCardClient : DummyClient() {
    override fun getNewCard(wiki: WikiSite?): Card {
        return WikiGamesCard()
    }
}
