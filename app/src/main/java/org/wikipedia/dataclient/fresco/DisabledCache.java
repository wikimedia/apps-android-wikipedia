package org.wikipedia.dataclient.fresco;

import android.support.annotation.NonNull;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.FileCache;
import com.facebook.imagepipeline.core.FileCacheFactory;

import java.io.IOException;

/**
 * A disabled dummy cache to provide to Fresco in order to prevent unwanted redundant disk caching.
 * OkHttp directly handles all of our caching needs thanks to CacheableOkHttpNetworkFetcher.
 */
public class DisabledCache implements FileCache {

    @NonNull
    public static FileCacheFactory factory() {
        return new DisabledCache.Factory();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public BinaryResource getResource(CacheKey cacheKey) {
        return null;
    }

    @Override
    public boolean hasKeySync(CacheKey cacheKey) {
        return false;
    }

    @Override
    public boolean hasKey(CacheKey cacheKey) {
        return false;
    }

    @Override
    public boolean probe(CacheKey cacheKey) {
        return false;
    }

    @Override
    public BinaryResource insert(CacheKey cacheKey, WriterCallback writerCallback) throws IOException {
        return null;
    }

    @Override
    public void remove(CacheKey cacheKey) {

    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public long getCount() {
        return 0;
    }

    @Override
    public long clearOldEntries(long l) {
        return 0;
    }

    @Override
    public void clearAll() {

    }

    @Override
    public DiskStorage.DiskDumpInfo getDumpInfo() throws IOException {
        return null;
    }

    @Override
    public void trimToMinimum() {

    }

    @Override
    public void trimToNothing() {

    }

    private static class Factory implements FileCacheFactory {
        @Override
        public FileCache get(DiskCacheConfig diskCacheConfig) {
            return new DisabledCache();
        }
    }
}
