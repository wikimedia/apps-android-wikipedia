package org.wikipedia.feed.random;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class RandomCard extends Card {
    @NonNull private Site site;

    public RandomCard(@NonNull Site site) {
        this.site = site;
    }

    @Override
    @NonNull
    public String title() {
        return "";
    }

    @NonNull @Override public CardType type() {
        return CardType.RANDOM;
    }

    public Site site() {
        return site;
    }
}