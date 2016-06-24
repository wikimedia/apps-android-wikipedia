package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;

public class MainPageCard extends Card {
    @NonNull private Site site;

    public MainPageCard(@NonNull Site site) {
        this.site = site;
    }

    @NonNull @Override public String title() {
        return "";
    }

    public Site site() {
        return site;
    }
}
