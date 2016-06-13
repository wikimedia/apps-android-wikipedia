package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class CardPageItem {
    @SuppressWarnings("NullableProblems") @NonNull private String title;
    @Nullable private Thumbnail thumbnail;
    @Nullable private String description;
    @Nullable private String extract;

    @NonNull
    public String title() {
        return title;
    }

    @Nullable
    public Uri thumbnail() {
        return thumbnail != null ? Uri.parse(thumbnail.source()) : null;
    }

    @Nullable
    public String description() {
        return description;
    }

    @Nullable
    public String extract() {
        return extract;
    }
}
