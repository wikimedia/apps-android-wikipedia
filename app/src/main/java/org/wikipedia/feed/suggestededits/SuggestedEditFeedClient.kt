package org.wikipedia.feed.suggestededits

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.DummyClient
import org.wikipedia.feed.model.Card

class SuggestedEditFeedClient : DummyClient() {

    override fun getNewCard(wiki: WikiSite): Card {
        return SuggestedEditCard(wiki)
    }

}
