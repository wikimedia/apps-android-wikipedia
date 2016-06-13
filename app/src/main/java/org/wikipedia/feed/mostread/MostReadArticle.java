package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.Thumbnail;

public final class MostReadArticle {
    @SerializedName("normalizedtitle") @SuppressWarnings("NullableProblems") @NonNull private String normalizedTitle;
    @SuppressWarnings("NullableProblems") @NonNull private String title;
    @Nullable private String description;
    @SerializedName("pageid") private int pageId;
    @SerializedName("thumbnail") @Nullable private Thumbnail thumbnail;
    private int rank;
    private int views;

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
        return thumbnail != null ? Uri.parse(thumbnail.source()) : null;
    }

    public int rank() {
        return rank;
    }

    public int views() {
        return views;
    }
}
