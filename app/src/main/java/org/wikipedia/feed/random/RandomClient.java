package org.wikipedia.feed.random;

import org.wikipedia.Site;
import org.wikipedia.feed.DummyClient;

public class RandomClient extends DummyClient<RandomCard> {
    public RandomClient() {
        super();
    }

    @Override
    public RandomCard getNewCard(Site site) {
        return new RandomCard(site);
    }
}
