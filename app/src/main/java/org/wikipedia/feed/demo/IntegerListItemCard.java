package org.wikipedia.feed.demo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.Card;

// todo: [Feed] remove.
public class IntegerListItemCard extends Card {
    private final int i;

    public IntegerListItemCard(int i) {
        this.i = i;
    }

    @NonNull @Override public String title() {
        return i + " The Revenant (2015 film)";
    }

    @Nullable @Override public String subtitle() {
        return "2015 film directed by Alejandro Gonzales";
    }
}