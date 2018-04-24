package org.wikipedia.feed.model;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;

import java.util.Collections;
import java.util.List;

public abstract class ListCard<T extends Card> extends WikiSiteCard {
    @NonNull private final List<T> items;

    public ListCard(@NonNull List<T> items, @NonNull WikiSite wiki) {
        super(wiki);
        this.items = Collections.unmodifiableList(items);
    }

    @NonNull public List<T> items() {
        return items;
    }
}
