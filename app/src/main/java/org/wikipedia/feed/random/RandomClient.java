package org.wikipedia.feed.random;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;

public class RandomClient extends DummyClient<RandomCard> {
    @Override
    public RandomCard getNewCard(WikiSite wiki) {
        return new RandomCard(wiki);
    }
}