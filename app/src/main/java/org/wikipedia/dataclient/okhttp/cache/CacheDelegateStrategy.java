package org.wikipedia.dataclient.okhttp.cache;

import android.support.annotation.NonNull;

import okhttp3.Request;

public final class CacheDelegateStrategy {
    public static boolean isCacheable(@NonNull Request request) {
        return SaveHeader.isSaveEnabled(request.header(SaveHeader.FIELD));
    }

    private CacheDelegateStrategy() { }
}
