package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

public final class MostReadArticle {
    @SerializedName("normalizedtitle") @SuppressWarnings("NullableProblems") @NonNull private String normalizedTitle;
    @SuppressWarnings("NullableProblems") @NonNull private String title;
    @Nullable private String description;
    @SerializedName("pageid") private int pageId;
    @SerializedName("thumbnail") @SuppressWarnings("NullableProblems") @NonNull private Map<Integer, URL> thumbnails;
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

    public Map<Integer, URL> thumbnails() {
        return thumbnails;
    }

    public int rank() {
        return rank;
    }

    public int views() {
        return views;
    }

    private MostReadArticle() {
        //noinspection ConstantConditions
        if (thumbnails == null) {
            thumbnails = Collections.emptyMap();
        }
    }
}