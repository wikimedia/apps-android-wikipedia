package org.wikipedia.feed.searchbar

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.offline.OfflineCard

class SearchClient : DummyClient() {
    override fun getNewCard(wiki: WikiSite?): Card {
        return SearchCard()
    }
}
