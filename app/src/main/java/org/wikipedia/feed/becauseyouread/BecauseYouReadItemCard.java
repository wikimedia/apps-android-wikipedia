package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.Card;

public class BecauseYouReadItemCard extends Card {
    @NonNull private final String title;
    @Nullable private final String subtitle;
    @Nullable private final Uri image;

    public BecauseYouReadItemCard(@NonNull String title, @Nullable String subtitle,
                                  @Nullable String image) {
        this.title = title;
        this.subtitle = subtitle;
        this.image = image != null ? Uri.parse(image) : null;
    }

    @NonNull
    @Override public String title() {
        return title;
    }

    @Nullable
    @Override public String subtitle() {
        return subtitle;
    }

    @Nullable
    @Override public Uri image() {
        return image;
    }
}
