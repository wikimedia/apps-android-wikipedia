package org.wikipedia.feed.wikiheader

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card

class WikiHeaderClient : DummyClient() {

    override fun getNewCard(wiki: WikiSite?): Card {
        return WikiHeaderCard()
    }
}