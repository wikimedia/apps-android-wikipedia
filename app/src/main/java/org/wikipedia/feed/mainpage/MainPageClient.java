package org.wikipedia.feed.mainpage;

import org.wikipedia.Site;
import org.wikipedia.feed.DummyClient;

public class MainPageClient extends DummyClient<MainPageCard> {
    public MainPageClient() {
        super();
    }

    @Override
    public MainPageCard getNewCard(Site site) {
        return new MainPageCard(site);
    }
}
