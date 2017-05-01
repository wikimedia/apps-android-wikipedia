package org.wikipedia.dataclient.okhttp.cache;

import android.support.annotation.Nullable;

import okhttp3.internal.cache.DiskLruCache;

public final class DiskLruCacheUtil {
    // DiskLruCache.valueCount is the number of files used per cache entry and
    // DiskLruCache.Snapshot. For OkHttp, the value is two. The first file is metadata (headers,
    // certificate, ...) and often ~8 KiB on disk. The second file is the raw response body which is
    // preserved as it was received with byte logicalSize equal to Content-Length header when positive.
    private static final int OKHTTP_METADATA_FILE_INDEX = 0;
    private static final int OKHTTP_RAW_BODY_FILE_INDEX = 1;

    /** @return The response metadata logicalSize in bytes. */
    public static long okHttpResponseMetadataSize(@Nullable DiskLruCache.Snapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }

        return snapshot.getLength(OKHTTP_METADATA_FILE_INDEX);
    }

    /** @return The response body logicalSize in bytes. */
    public static long okHttpResponseBodySize(@Nullable DiskLruCache.Snapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }

        return snapshot.getLength(OKHTTP_RAW_BODY_FILE_INDEX);
    }

    private DiskLruCacheUtil() { }
}
