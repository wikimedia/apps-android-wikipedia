package org.wikipedia.feed.model;

import android.support.annotation.NonNull;

public class Thumbnail {
    @SuppressWarnings("NullableProblems") @NonNull private String source;
    private int height;
    private int width;

    @NonNull
    public String source() {
        return source;
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }
}
