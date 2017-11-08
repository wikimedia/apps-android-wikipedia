package org.wikipedia.offline;

import android.net.Uri;
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
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.util.DateUtil.getIso8601DateFormatShort;

public class Compilation {
    public static final String MIME_TYPE = "application/zim";
    private static final int COMPRESSION_DICT_SIZE = 2 * 1024 * 1024;

    @Nullable private String name;
    @Nullable private Uri uri;
    @Nullable private List<String> langCodes;
    @Nullable private String summary;
    @Nullable private String description;
    @Nullable private MediaContent media;
    @Nullable private Image thumb;
    @Nullable private Image featureImage;
    private long count;
    private long size; // bytes
    @NonNull private Date date = new Date();

    @Nullable private String path;
    @Nullable private transient ZimFile file;
    @Nullable private transient ZimReader reader;
    @Nullable private transient String mainPageTitle;

    public enum MediaContent {
        ALL, NOVID, NOPIC
    }

    public Compilation() {
    }

    Compilation(@NonNull File file) throws IOException {
        path = file.getAbsolutePath();
        this.file = new ZimFile(path);
        reader = new ZimReader(this.file);
        reader.setLzmaDictSize(COMPRESSION_DICT_SIZE);
    }

    @VisibleForTesting
    Compilation(@NonNull File file, LruCache titleCache, LruCache urlCache) throws Exception {
        path = file.getAbsolutePath();
        this.file = new ZimFile(path);
        reader = new ZimReader(this.file, titleCache, urlCache);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    Compilation(@NonNull String[] data) {
        this.name = data[0];
        this.uri = Uri.parse(data[1]);
        this.langCodes = Collections.singletonList(data[2]);
        this.summary = data[3];
        this.description = data[4];
        this.media = MediaContent.valueOf(data[5]);
        this.thumb = new Image(Uri.parse(data[6]), 0, 0);
        this.featureImage = new Image(Uri.parse(data[7]), 0, 0);
        this.count = 0;  // currently unused
        this.size = Long.parseLong(data[9]);
        try {
            this.date = getIso8601DateFormatShort().parse(data[10]);
        } catch (ParseException e) {
            L.e(e);
        }
    }

    public void copyMetadataFrom(@NonNull Compilation other) {
        name = other.name();
        uri = other.uri();
        langCodes = other.langCodes();
        summary = other.summary();
        description = other.description();
        media = other.mediaContent();
        thumb = other.thumb();
        featureImage = other.featureImage();
        count = other.count();
        size = other.size();
        date = other.date();
    }

    public boolean pathNameMatchesUri(@Nullable Uri otherUri) {
        if (file == null || otherUri == null) {
            return false;
        }
        return file.getName().equals(otherUri.getLastPathSegment());
    }

    public boolean uriNameMatchesUri(@Nullable Uri otherUri) {
        if (uri == null || otherUri == null) {
            return false;
        }
        return uri.getLastPathSegment().equals(otherUri.getLastPathSegment());
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
        return file != null ? file.getAbsolutePath() : defaultString(path);
    }

    public boolean existsOnDisk() {
        return !TextUtils.isEmpty(path());
    }

    public long size() {
        return file != null && size == 0 ? file.length() : size;
    }

    @NonNull
    public Date date() {
        if (reader != null) {
            date = reader.getZimDate();
        } else if (file != null) {
            date = new Date(file.lastModified());
        }
        return date;
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
        return defaultString(name, "");
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
        return defaultString(description, "");
    }

    @Nullable
    public String summary() {
        return summary;
    }

    @Nullable
    public Image thumb() {
        return thumb;
    }

    @Nullable
    public Uri thumbUri() {
        return thumb != null ? thumb.uri() : null;
    }

    @Nullable
    public Image featureImage() {
        return featureImage;
    }

    @Nullable
    public Uri featureImageUri() {
        return featureImage != null ? featureImage.uri() : null;
    }

    public long count() {
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
        return reader == null ? null : reader.getDataForUrl(URLDecoder.decode(url, "utf-8"));
    }

    @NonNull
    String getRandomTitle() throws IOException {
        return reader.getRandomTitle();
    }

    @NonNull
    String getMainPageTitle() throws IOException {
        if (mainPageTitle == null) {
            mainPageTitle = reader.getMainPageTitle();
        }
        return mainPageTitle;
    }

    public static class Image {
        @NonNull private Uri uri;
        private int width;
        private int height;

        Image(@NonNull Uri uri, int width, int height) {
            this.uri = uri;
            this.width = width;
            this.height = height;
        }

        public Uri uri() {
            return uri;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }
}
