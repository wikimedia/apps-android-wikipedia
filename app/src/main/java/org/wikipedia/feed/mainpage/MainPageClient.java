package org.wikipedia.feed.mainpage;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;

public class MainPageClient extends DummyClient<MainPageCard> {
    public MainPageClient() {
        super();
    }

    @Override
    public MainPageCard getNewCard(WikiSite wiki) {
        return new MainPageCard(wiki);
    }
}
