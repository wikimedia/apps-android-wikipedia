package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;

public final class Thumbnail {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Uri source;
    @SuppressWarnings("unused") private int height;
    @SuppressWarnings("unused") private int width;

    @NonNull
    public Uri source() {
        return source;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
