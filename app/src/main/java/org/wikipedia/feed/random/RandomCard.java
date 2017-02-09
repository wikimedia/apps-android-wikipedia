package org.wikipedia.feed.random;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class RandomCard extends Card {
    @NonNull private WikiSite wiki;

    public RandomCard(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @NonNull @Override public CardType type() {
        return CardType.RANDOM;
    }

    public WikiSite wikiSite() {
        return wiki;
    }
}
