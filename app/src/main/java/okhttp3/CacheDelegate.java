package okhttp3;

import org.wikipedia.util.FileUtil;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
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

    public void delete() {
        FileUtil.deleteRecursively(cache.cache.getDirectory());
    }

    public boolean moveCacheFilesToDirectory(@NonNull String url, @NonNull CacheDelegate destination) {
        String metadataFileName = "/" + key(url) + "." + OKHTTP_METADATA_FILE_INDEX;
        String rawBodyFileName = "/" + key(url) + "." + OKHTTP_RAW_BODY_FILE_INDEX;
        File metadata = new File (cache.cache.getDirectory() + metadataFileName);
        File rawBody = new File (cache.cache.getDirectory() + rawBodyFileName);
        return metadata.renameTo(new File(destination.cache.cache.getDirectory() + metadataFileName))
                && rawBody.renameTo(new File(destination.cache.cache.getDirectory() + rawBodyFileName));
    }

    public boolean moveAllCacheFilesToDirectory(@NonNull CacheDelegate destination) {
        boolean result;
        String targetDirectoryPath = destination.cache.cache.getDirectory().getPath();
        destination.delete();
        result = cache.cache.getDirectory().renameTo(new File(targetDirectoryPath));
        delete();
        return result;
    }

    // Copy of Cache.key()
    @NonNull private String key(@NonNull String url) {
        return ByteString.encodeUtf8(url).md5().hex();
    }
}
