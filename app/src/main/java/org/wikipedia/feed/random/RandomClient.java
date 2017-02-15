package org.wikipedia.feed.random;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;

public class RandomClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new RandomCard(wiki);
    }
}
