package org.wikipedia.feed.searchbar;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;

public class SearchCard extends Card {
    public SearchCard() {
    }

    @Override @NonNull
    public String title() {
        return "search";
    }
}