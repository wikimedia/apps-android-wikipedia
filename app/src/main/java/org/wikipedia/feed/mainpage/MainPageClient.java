package org.wikipedia.feed.mainpage;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;

public class MainPageClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new MainPageCard();
    }
}
