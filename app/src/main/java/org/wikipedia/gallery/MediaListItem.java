package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@SuppressWarnings("unused")
public class MediaListItem implements Serializable {
    @Nullable private String title;
    @SerializedName("section_id") private int sectionId;
    @Nullable private String type;
    @Nullable private TextInfo caption;
    private boolean showInGallery;
    @SuppressWarnings("unused") private Image[] srcset;
    @SuppressWarnings("audio_type") private String audioType;
    private static final int SMALL = 320;
    private static final int MEDIUM = 640;
    private static final int LARGE = 800;
    private static final int XL = 1024;

    public MediaListItem() {
    }

    public MediaListItem(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getType() {
        return StringUtils.defaultString(type);
    }

    @Nullable
    public TextInfo getCaption() {
        return caption;
    }

    public boolean showInGallery() {
        return showInGallery;
    }

    @NonNull
    public String getTitle() {
        return StringUtils.defaultString(title);
    }

    public String getAudioType() {
        return audioType;
    }

    public static class Image {
        @SuppressWarnings("unused")
        @SerializedName("src")
        private String url;
        @SuppressWarnings("unused")
        @SerializedName("scale")
        private String size;
    }

    @Nullable
    public String getImageUrlFor(int width) {
        switch (width) {
            case SMALL:
            case MEDIUM:
                return getSrcSetUrlFor("1x");
            case LARGE:
                return getSrcSetUrlFor("1.5x");
            case XL:
                return getSrcSetUrlFor("2x");
            default:
                return null;
        }
    }

    private String getSrcSetUrlFor(String scale) {
        for (Image image : srcset) {
            if (image.size.equals(scale)) {
                return image.url;
            }
        }
        return null;
    }
}
