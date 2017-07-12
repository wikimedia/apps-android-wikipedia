package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.LruCache;

import com.dmitrybrant.zimdroid.ZimFile;
import com.dmitrybrant.zimdroid.ZimReader;

import org.wikipedia.util.log.L;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

public class Compilation {
    @NonNull private ZimFile file;
    @NonNull private ZimReader reader;

    public Compilation(@NonNull File file) throws IOException {
        this.file = new ZimFile(file.getAbsolutePath());
        reader = new ZimReader(this.file);
    }

    @VisibleForTesting
    Compilation(@NonNull File file, LruCache titleCache, LruCache urlCache) throws Exception {
        this.file = new ZimFile(file.getAbsolutePath());
        reader = new ZimReader(this.file, titleCache, urlCache);
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // close silently
        }
    }

    @NonNull public String path() {
        return file.getAbsolutePath();
    }

    public long size() {
        return file.length();
    }

    @NonNull public String name() {
        try {
            return reader.getZimTitle();
        } catch (IOException e) {
            L.e(e);
        }
        return "";
    }

    @NonNull public String description() {
        try {
            return reader.getZimDescription();
        } catch (IOException e) {
            L.e(e);
        }
        return "";
    }

    @NonNull public List<String> searchByPrefix(@NonNull String prefix, int maxResults) throws IOException {
        return reader.searchByPrefix(prefix, maxResults);
    }

    public boolean titleExists(@NonNull String title) {
        return !TextUtils.isEmpty(getNormalizedTitle(title));
    }

    @Nullable public String getNormalizedTitle(@NonNull String title) {
        try {
            return reader.getNormalizedTitle(title);
        } catch (Exception e) {
            L.e(e);
        }
        return null;
    }

    @Nullable public ByteArrayOutputStream getDataForTitle(@NonNull String title) throws IOException {
        return reader.getDataForTitle(title);
    }

    @Nullable public ByteArrayOutputStream getDataForUrl(@NonNull String url) throws IOException {
        if (url.startsWith("A/") || url.startsWith("I/")) {
            url = url.substring(2);
        }
        return reader.getDataForUrl(URLDecoder.decode(url, "utf-8"));
    }

    @NonNull public String getRandomTitle() throws IOException {
        return reader.getRandomTitle();
    }

    @NonNull public String getMainPageTitle() throws IOException {
        return reader.getMainPageTitle();
    }
}
