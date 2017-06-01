package okhttp3;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.internal.Util;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.cache.InternalCache;
import okio.ByteString;

public class CacheDelegate {
    @NonNull public static InternalCache internalCache(@NonNull Cache cache) {
        return cache.internalCache;
    }

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

    @NonNull public DiskLruCache diskLruCache() {
        return cache.cache;
    }

    @Nullable public DiskLruCache.Snapshot entry(@NonNull Request req) {
        try {
            return cache.cache.get(key(req.url().toString()));
        } catch (IOException ignore) {
            return null;
        }
    }

    // Copy of Cache.get(). Calling this method modifies the Cache. If the URL is present, it's
    // cache entry is moved to the head of the LRU queue. This method performs file I/O
    public boolean isCached(@NonNull String url) {
        String key = key(url);
        DiskLruCache.Snapshot snapshot;
        try {
            snapshot = cache.cache.get(key);
            if (snapshot == null) {
                return false;
            }
        } catch (IOException e) {
            // Give up because the cache cannot be read.
            return false;
        }

        Util.closeQuietly(snapshot);
        return true;
    }

    // Copy of Cache.remove(). This method performs file I/O
    public void remove(@NonNull Request req) {
        try {
            cache.remove(req);
        } catch (IOException ignore) { }
    }

    // Copy of Cache.key()
    @NonNull private String key(@NonNull String url) {
        return ByteString.encodeUtf8(url).md5().hex();
    }
}
