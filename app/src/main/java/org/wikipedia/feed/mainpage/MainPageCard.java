package org.wikipedia.feed.mainpage;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;

public class MainPageCard extends WikiSiteCard {
    public MainPageCard(@NonNull WikiSite wiki) {
        super(wiki);
    }

    @NonNull @Override public CardType type() {
        return CardType.MAIN_PAGE;
    }
}
