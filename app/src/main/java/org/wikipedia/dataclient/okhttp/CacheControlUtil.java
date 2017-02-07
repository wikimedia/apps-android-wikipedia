package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;

final class CacheControlUtil {
    @NonNull static CacheControl cacheControlWithDefaultMaximumStale(@NonNull CacheControl cacheControl) {
        int maxStaleSeconds = cacheControl.maxStaleSeconds() < 0
                ? Integer.MAX_VALUE
                : cacheControl.maxStaleSeconds();
        return newCacheControlBuilder(cacheControl)
                .maxStale(maxStaleSeconds, TimeUnit.SECONDS)
                .build();
    }

    @NonNull private static CacheControl.Builder newCacheControlBuilder(@NonNull CacheControl cacheControl) {
        CacheControl.Builder builder = new CacheControl.Builder();
        if (cacheControl.noCache()) {
            builder.noCache();
        }
        if (cacheControl.noStore()) {
            builder.noStore();
        }
        if (cacheControl.maxAgeSeconds() >= 0) {
            builder.maxAge(cacheControl.maxAgeSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.maxStaleSeconds() >= 0) {
            builder.maxStale(cacheControl.maxStaleSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.minFreshSeconds() >= 0) {
            builder.minFresh(cacheControl.minFreshSeconds(), TimeUnit.SECONDS);
        }
        if (cacheControl.onlyIfCached()) {
            builder.onlyIfCached();
        }
        if (cacheControl.noTransform()) {
            builder.noTransform();
        }
        return builder;
    }

    private CacheControlUtil() { }
}