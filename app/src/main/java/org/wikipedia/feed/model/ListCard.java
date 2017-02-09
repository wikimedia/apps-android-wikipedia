package org.wikipedia.feed.model;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public abstract class ListCard<T extends Card> extends Card {
    @NonNull private final List<T> items;

    public ListCard(@NonNull List<T> items) {
        this.items = Collections.unmodifiableList(items);
    }

    @NonNull public List<T> items() {
        return items;
    }
}
