package org.wikipedia.feed.model;

import androidx.annotation.NonNull;

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
