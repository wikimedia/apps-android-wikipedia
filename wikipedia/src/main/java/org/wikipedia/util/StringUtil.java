package org.wikipedia.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// TODO: Replace with Apache Commons Lang StringUtils.
public final class StringUtil {
    @NonNull
    public static String emptyIfNull(@Nullable String value) {
        return defaultIfNull(value, "");
    }

    @NonNull
    public static CharSequence emptyIfNull(@Nullable CharSequence value) {
        return defaultIfNull(value, "");
    }

    @Nullable
    public static String defaultIfNull(@Nullable String value, @Nullable String defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static CharSequence defaultIfNull(@Nullable CharSequence value,
            @Nullable CharSequence defaultValue) {
        return value == null ? defaultValue : value;
    }

    private StringUtil() { }
}