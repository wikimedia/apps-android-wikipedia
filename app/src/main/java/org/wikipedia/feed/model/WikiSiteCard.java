package org.wikipedia.feed.model;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;

public abstract class WikiSiteCard extends Card {
    @NonNull private WikiSite wiki;

    public WikiSiteCard(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @NonNull public WikiSite wikiSite() {
        return wiki;
    }
}
