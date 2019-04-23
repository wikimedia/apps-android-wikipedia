package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
public class ImageInfo implements Serializable {
    @SuppressWarnings("unused") private int size;
    @SuppressWarnings("unused") private int width;
    @SuppressWarnings("unused") private int height;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String source;
    @SuppressWarnings("unused") @SerializedName("thumburl") @Nullable private String thumbUrl;
    @SuppressWarnings("unused") @SerializedName("thumbwidth") private int thumbWidth;
    @SuppressWarnings("unused") @SerializedName("thumbheight") private int thumbHeight;
    @SuppressWarnings("unused") @SerializedName("url") @Nullable private String originalUrl;
    @SuppressWarnings("unused") @SerializedName("descriptionurl") @Nullable private String descriptionUrl;
    @SuppressWarnings("unused") @SerializedName("descriptionshorturl") @Nullable private String descriptionShortUrl;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("mime") @NonNull private String mimeType = "*/*";
    @SuppressWarnings("unused") @SerializedName("extmetadata")@Nullable private ExtMetadata metadata;

    @NonNull
    public String getSource() {
        return StringUtils.defaultString(source);
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @NonNull public String getThumbUrl() {
        return StringUtils.defaultString(thumbUrl);
    }

    @NonNull public String getOriginalUrl() {
        return StringUtils.defaultString(originalUrl);
    }

    @Nullable public ExtMetadata getMetadata() {
        return metadata;
    }
}
