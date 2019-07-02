package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
@SuppressWarnings("unused")
public class ImageInfo implements Serializable {
    private int size;
    private int width;
    private int height;
    @Nullable private String source;
    @SerializedName("thumburl") @Nullable private String thumbUrl;
    @SerializedName("thumbwidth") private int thumbWidth;
    @SerializedName("thumbheight") private int thumbHeight;
    @SerializedName("url") @Nullable private String originalUrl;
    @SerializedName("descriptionurl") @Nullable private String descriptionUrl;
    @SerializedName("descriptionshorturl") @Nullable private String descriptionShortUrl;
    @SerializedName("mime") @Nullable private String mimeType;
    @SerializedName("extmetadata")@Nullable private ExtMetadata metadata;
    @Nullable private String user;
    @Nullable private String timestamp;

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

    @NonNull public String getMimeType() {
        return StringUtils.defaultString(mimeType, "*/*");
    }

    @NonNull public String getThumbUrl() {
        return StringUtils.defaultString(thumbUrl);
    }

    @NonNull public String getOriginalUrl() {
        return StringUtils.defaultString(originalUrl);
    }

    @NonNull public String getUser() {
        return StringUtils.defaultString(user);
    }

    @NonNull public String getTimestamp() {
        return StringUtils.defaultString(timestamp);
    }

    @Nullable public ExtMetadata getMetadata() {
        return metadata;
    }
}
