package org.wikipedia.feed.model;

import org.wikipedia.dataclient.WikiSite;

import androidx.annotation.NonNull;

public abstract class WikiSiteCard extends Card {
    @NonNull private WikiSite wiki;

    public WikiSiteCard(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @NonNull public WikiSite wikiSite() {
        return wiki;
    }
}
