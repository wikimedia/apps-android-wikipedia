package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.Thumbnail;

public final class MostReadArticle {
    @SerializedName("normalizedtitle") @SuppressWarnings("unused,NullableProblems") @NonNull private String normalizedTitle;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @SerializedName("pageid") private int pageId;
    @SuppressWarnings("unused") @SerializedName("thumbnail") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") private int rank;
    @SuppressWarnings("unused") private int views;

    @NonNull public String normalizedTitle() {
        return normalizedTitle;
    }

    @NonNull public String title() {
        return title;
    }

    @Nullable public String description() {
        return description;
    }

    public int pageId() {
        return pageId;
    }

    public Uri thumbnail() {
        return thumbnail != null ? thumbnail.source() : null;
    }

    public int rank() {
        return rank;
    }

    public int views() {
        return views;
    }
}
