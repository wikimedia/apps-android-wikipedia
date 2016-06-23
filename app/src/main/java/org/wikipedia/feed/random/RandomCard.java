package org.wikipedia.feed.random;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;

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

    public Site site() {
        return site;
    }
}
