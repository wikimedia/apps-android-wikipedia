package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;

public class MainPageCard extends Card {
    @NonNull private WikiSite wiki;

    public MainPageCard(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return "";
    }

    @NonNull @Override public CardType type() {
        return CardType.MAIN_PAGE;
    }

    public WikiSite wikiSite() {
        return wiki;
    }
}
