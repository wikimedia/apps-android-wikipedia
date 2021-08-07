package org.wikipedia.json;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;

public class MoshiUtil {
    private static final Moshi DEFAULT_MOSHI_BUILDER = new Moshi.Builder().build();

    @NonNull
    public static Moshi getDefaultMoshi() {
        return DEFAULT_MOSHI_BUILDER;
    }
}
