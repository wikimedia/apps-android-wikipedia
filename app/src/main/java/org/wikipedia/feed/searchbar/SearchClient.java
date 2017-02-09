package org.wikipedia.feed.searchbar;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;

public class SearchClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new SearchCard();
    }
}
