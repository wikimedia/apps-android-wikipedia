package org.wikipedia.offline;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;

// A Gson POJO that will model the compilations endpoint response. TODO: Finalize when the spec is defined.
class CompilationInfo {
    @SuppressWarnings("unused") private String name;
    @SuppressWarnings("unused,NullableProblems") @NonNull private Uri uri;
    @SuppressWarnings("unused") @Nullable private List<String> langCodes;
    @SuppressWarnings("unused") @Nullable private String summary;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private MediaContent media;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("thumb_url") @Nullable private Image thumb;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("image_url") @Nullable private Image image; // full-size image
    @SuppressWarnings("unused") private int count;
    @SuppressWarnings("unused") private long size; // bytes
    @SuppressWarnings("unused") private long timestamp;

    // TODO: Constructor for development/testing only, remove when no longer needed
    @SuppressWarnings("checkstyle:parameternumber")
    CompilationInfo(@NonNull String name, @NonNull Uri uri, @Nullable List<String> langCodes,
                    @Nullable String summary, @Nullable String description, @Nullable MediaContent media,
                    @Nullable Image thumb, @Nullable Image image, int count, long size, long timestamp) {
        this.name = name;
        this.uri = uri;
        this.langCodes = langCodes;
        this.summary = summary;
        this.description = description;
        this.media = media;
        this.thumb = thumb;
        this.image = image;
        this.count = count;
        this.size = size;
        this.timestamp = timestamp;
    }

    @NonNull
    public String name() {
        return name;
    }

    @NonNull
    public Uri uri() {
        return uri;
    }

    @Nullable
    public String summary() {
        return summary;
    }

    @Nullable
    public String description() {
        return description;
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
    public Image image() {
        return image;
    }

    @Nullable
    public Uri imageUri() {
        return image != null ? image.uri() : null;
    }

    public int count() {
        return count;
    }

    public long size() {
        return size;
    }

    @Nullable
    public MediaContent mediaContent() {
        return media;
    }

    public long timestamp() {
        return timestamp;
    }

    public static class Image {
        @NonNull private Uri uri;
        private int width;
        private int height;

        Image(@Nullable String uri, int width, int height) {
            this(Uri.parse(defaultString(uri)), width, height);
        }

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

    public enum MediaContent {
        ALL, IMAGES, NONE
    }

    // TODO: Below functions are for dev only, remove when finished!
    @SuppressWarnings("checkstyle:magicnumber")
    public static CompilationInfo getMockInfoForTesting() {
        return new CompilationInfo("Bollywood (English Wikipedia, Feb 2017)",
                Uri.parse("https://download.kiwix.org/zim/wikipedia/wikipedia_en_bollywood_nopic_2017-02.zim"),
                Collections.singletonList("en"), "Articles about Bollywood from English Wikipedia",
                "Articles about Bollywood from English Wikipedia. Articles about Bollywood from English Wikipedia. "
                        + "Articles about Bollywood from English Wikipedia. Articles about Bollywood from English Wikipedia. "
                        + "Articles about Bollywood from English Wikipedia.", CompilationInfo.MediaContent.ALL,
                getBollywoodThumb(), getBollywoodImage(), 10000, 104000000, 1487065620);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static CompilationInfo.Image getBollywoodImage() {
        return new CompilationInfo.Image("https://upload.wikimedia.org/wikipedia/commons/2/26/Bollywood_dance_show_in_Bristol.jpg", 1024, 768);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static CompilationInfo.Image getBollywoodThumb() {
        return new CompilationInfo.Image("https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/Bollywood_dance_show_in_Bristol.jpg/320px-Bollywood_dance_show_in_Bristol.jpg", 320, 240);
    }
}
