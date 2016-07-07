package org.wikipedia.feed.random;

import org.wikipedia.Site;
import org.wikipedia.feed.DummyClient;

public class RandomClient extends DummyClient<RandomCard> {
    @Override
    public RandomCard getNewCard(Site site) {
        return new RandomCard(site);
    }
}