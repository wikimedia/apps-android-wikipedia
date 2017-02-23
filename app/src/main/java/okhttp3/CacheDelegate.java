package okhttp3;

import android.support.annotation.NonNull;

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
