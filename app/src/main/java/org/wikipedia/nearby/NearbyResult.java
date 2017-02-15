package org.wikipedia.nearby;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;

import java.util.List;

class NearbyResult {
    @NonNull private final WikiSite wiki;
    @NonNull private final List<NearbyPage> list;

    NearbyResult(@NonNull WikiSite wiki, @NonNull List<NearbyPage> list) {
        this.wiki = wiki;
        this.list = list;
    }

    @NonNull public WikiSite getWiki() {
        return wiki;
    }

    @NonNull public List<NearbyPage> getList() {
        return list;
    }
}
