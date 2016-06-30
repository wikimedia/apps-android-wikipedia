package org.wikipedia.feed.model;

import android.support.annotation.NonNull;

public final class Thumbnail {
    @SuppressWarnings("unused,NullableProblems") @NonNull private String source;
    @SuppressWarnings("unused") private int height;
    @SuppressWarnings("unused") private int width;

    @NonNull
    public String source() {
        return source;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
