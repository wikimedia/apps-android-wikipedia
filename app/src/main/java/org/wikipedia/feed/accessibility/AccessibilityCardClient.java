package org.wikipedia.feed.accessibility;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;

public class AccessibilityCardClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new AccessibilityCard();
    }
}
