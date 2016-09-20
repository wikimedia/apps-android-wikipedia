package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class MainPageCard extends Card {
    @NonNull private Site site;

    public MainPageCard(@NonNull Site site) {
        this.site = site;
    }

    @NonNull @Override public String title() {
        return "";
    }

    @NonNull @Override public CardType type() {
        return CardType.MAIN_PAGE;
    }

    public Site site() {
        return site;
    }
}
