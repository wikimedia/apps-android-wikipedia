package okhttp3;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.internal.Util;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.cache.InternalCache;
import okio.ByteString;

public class CacheDelegate {
    private static final int OKHTTP_METADATA_FILE_INDEX = 0;
    private static final int OKHTTP_RAW_BODY_FILE_INDEX = 1;

    @NonNull private final Cache cache;

    public CacheDelegate(@NonNull Cache cache) {
        this.cache = cache;
        if (!cache.cache.getDirectory().exists()) {
            // When upgrading from previous versions where this cache didn't yet exist,
            // make sure that the cache directory is created first.
            // For example, SavedPageSyncService will immediately call Stat on this directory,
            // which will cause a crash if it doesn't exist.
            cache.cache.getDirectory().mkdirs();
        }
    }

    @NonNull public InternalCache internalCache() {
        return cache.internalCache;
    }

    // Copy of Cache.get(). Calling this method modifies the Cache. If the URL is present, its
    // cache entry is moved to the head of the LRU queue.
    public boolean isCached(@NonNull String url) {
        try {
            DiskLruCache.Snapshot snapshot = cache.cache.get(key(url));
            if (snapshot != null) {
                Util.closeQuietly(snapshot);
                return true;
            }
        } catch (IOException e) {
            // cache cannot be read.
        }
        return false;
    }

    public long getSizeOnDisk(@NonNull Request req) {
        long totalSize = 0;
        try {
            DiskLruCache.Snapshot snapshot = cache.cache.get(key(req.url().toString()));
            if (snapshot != null) {
                totalSize += snapshot.getLength(OKHTTP_METADATA_FILE_INDEX);
                totalSize += snapshot.getLength(OKHTTP_RAW_BODY_FILE_INDEX);
            }
        } catch (IOException ignore) {
            //
        }
        return totalSize;
    }

    // Copy of Cache.key()
    @NonNull private String key(@NonNull String url) {
        return ByteString.encodeUtf8(url).md5().hex();
    }
}
