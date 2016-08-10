package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.view.FeedRecyclerAdapter;
import org.wikipedia.model.BaseModel;

public abstract class Card extends BaseModel {
    @NonNull public abstract String title();

    @Nullable public String subtitle() {
        return null;
    }

    @Nullable public Uri image() {
        return null;
    }

    @Nullable public String extract() {
        return null;
    }

    // TODO: enforce a contract for subclasses of Card to implement hashCode()
    public String getHideKey() {
        return Long.toString(FeedRecyclerAdapter.getCardType(this) + hashCode());
    }
}