package org.wikipedia.offline;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.LruCache;

import com.dmitrybrant.zimdroid.ZimFile;
import com.dmitrybrant.zimdroid.ZimReader;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.log.L;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Compilation {
    @Nullable private String name;
    @Nullable private Uri uri;
    @Nullable private List<String> langCodes;
    @Nullable private String summary;
    @Nullable private String description;
    @Nullable private MediaContent media;
    @SerializedName("thumb_url") @Nullable private String thumbUrl;
    @SerializedName("image_url") @Nullable private String imageUrl;
    private int count;
    private long size; // bytes
    private long timestamp;

    @Nullable private String path;
    @Nullable private transient ZimFile file;
    @Nullable private transient ZimReader reader;

    public enum MediaContent {
        ALL, IMAGES, NONE
    }

    Compilation(@NonNull File file) throws IOException {
        path = file.getAbsolutePath();
        this.file = new ZimFile(path);
        reader = new ZimReader(this.file);
    }

    @VisibleForTesting
    Compilation(@NonNull File file, LruCache titleCache, LruCache urlCache) throws Exception {
        path = file.getAbsolutePath();
        this.file = new ZimFile(path);
        reader = new ZimReader(this.file, titleCache, urlCache);
    }

    // TODO: Constructor for development/testing only, remove when no longer needed
    @SuppressWarnings("checkstyle:parameternumber")
    Compilation(@NonNull String name, @NonNull Uri uri, @Nullable List<String> langCodes,
                @Nullable String summary, @Nullable String description, @Nullable MediaContent media,
                @Nullable String thumbUrl, @Nullable String imageUrl, int count, long size, long timestamp) {
        this.name = name;
        this.uri = uri;
        this.langCodes = langCodes;
        this.summary = summary;
        this.description = description;
        this.media = media;
        this.thumbUrl = thumbUrl;
        this.imageUrl = imageUrl;
        this.count = count;
        this.size = size;
        this.timestamp = timestamp;
    }

    public void copyMetadataFrom(@NonNull Compilation other) {
        name = other.name();
        uri = other.uri();
        langCodes = other.langCodes();
        summary = other.summary();
        description = other.description();
        media = other.mediaContent();
        thumbUrl = other.thumbUrl();
        imageUrl = other.imageUrl();
        count = other.count();
        size = other.size();
        timestamp = other.timestamp();
    }

    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            // close silently
        }
    }

    @Nullable
    public Uri uri() {
        return uri;
    }

    @Nullable
    public List<String> langCodes() {
        return langCodes;
    }

    @NonNull
    public String path() {
        return file != null ? file.getAbsolutePath() : StringUtils.defaultString(path);
    }

    public long size() {
        return file != null && size == 0 ? file.length() : size;
    }

    public long timestamp() {
        return file != null && timestamp == 0 ? file.lastModified() : timestamp;
    }

    @NonNull
    public String name() {
        try {
            if (reader != null && TextUtils.isEmpty(name)) {
                return reader.getZimTitle();
            }
        } catch (IOException e) {
            L.e(e);
        }
        return StringUtils.defaultString(name, "");
    }

    @NonNull
    public String description() {
        try {
            if (reader != null && TextUtils.isEmpty(description)) {
                return reader.getZimDescription();
            }
        } catch (IOException e) {
            L.e(e);
        }
        return StringUtils.defaultString(description, "");
    }

    @Nullable
    public String summary() {
        return summary;
    }

    @Nullable
    public String thumbUrl() {
        return thumbUrl;
    }

    @Nullable
    public String imageUrl() {
        return imageUrl;
    }

    public int count() {
        return count;
    }

    @Nullable
    public MediaContent mediaContent() {
        return media;
    }

    @NonNull
    List<String> searchByPrefix(@NonNull String prefix, int maxResults) throws IOException {
        return reader != null ? reader.searchByPrefix(prefix, maxResults) : Collections.<String>emptyList();
    }

    boolean titleExists(@NonNull String title) {
        return !TextUtils.isEmpty(getNormalizedTitle(title));
    }

    @Nullable
    public String getNormalizedTitle(@NonNull String title) {
        try {
            if (reader != null) {
                return reader.getNormalizedTitle(title);
            }
        } catch (Exception e) {
            L.e(e);
        }
        return null;
    }

    @Nullable
    ByteArrayOutputStream getDataForTitle(@NonNull String title) throws IOException {
        return reader == null ? null : reader.getDataForTitle(title);
    }

    @Nullable
    ByteArrayOutputStream getDataForUrl(@NonNull String url) throws IOException {
        if (url.startsWith("A/") || url.startsWith("I/")) {
            url = url.substring(2);
        }
        return reader == null ? null : reader.getDataForUrl(URLDecoder.decode(url, "utf-8"));
    }

    @NonNull
    String getRandomTitle() throws IOException {
        return reader.getRandomTitle();
    }

    @NonNull
    String getMainPageTitle() throws IOException {
        return reader.getMainPageTitle();
    }

    // TODO: Below functions are for dev only, remove when finished!
    @SuppressWarnings("checkstyle:magicnumber")
    static List<Compilation> getMockInfoForTesting() {
        return Arrays.asList(
                new Compilation("Wikipedia (English) top 45000 articles, Feb 2017",
                        Uri.parse("https://download.kiwix.org/zim/wikipedia/wikipedia_en_wp1-0.8_2017-02.zim"),
                        Collections.singletonList("en"),
                        "Compilation of the top 45000 articles, by popularity, from English Wikipedia.",
                        "Compilation of the top 45000 articles, by popularity, from English Wikipedia.",
                        Compilation.MediaContent.ALL,
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Wikipedia-logo-v2-en.svg/200px-Wikipedia-logo-v2-en.svg.png",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Wikipedia-logo-v2-en.svg/500px-Wikipedia-logo-v2-en.svg.png",
                        10000, 5453656823L, 1487065620),
                new Compilation("WikiProject Medicine (English), Jun 2017",
                        Uri.parse("https://download.kiwix.org/zim/wikipedia/wikipedia_en_medicine_novid_2017-06.zim"),
                        Collections.singletonList("en"),
                        "Compilation of all articles from WikiProject Medicine, from English Wikipedia.",
                        "Compilation of all articles from WikiProject Medicine, from English Wikipedia.",
                        Compilation.MediaContent.ALL,
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Wikipedia-logo-v2-en.svg/200px-Wikipedia-logo-v2-en.svg.png",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Wikipedia-logo-v2-en.svg/500px-Wikipedia-logo-v2-en.svg.png",
                        10000, 1175000000L, 1487065620)
        );
    }
}
