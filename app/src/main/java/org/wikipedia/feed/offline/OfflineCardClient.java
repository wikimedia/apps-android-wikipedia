package org.wikipedia.feed.offline;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;

public class OfflineCardClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new OfflineCard();
    }
}
