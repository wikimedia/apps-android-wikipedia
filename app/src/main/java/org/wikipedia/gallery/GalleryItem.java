package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class GalleryItem {
    @NonNull private String name;
    @Nullable private String url;
    @NonNull private String mimeType;
    @Nullable private ExtMetadata metadata;
    @Nullable private String thumbUrl;
    private int width;
    private int height;
    @NonNull private ImageLicense license;
    @Nullable private List<Derivative> derivatives;

    public GalleryItem(@NonNull String title, @NonNull ImageInfo imageInfo) {
        boolean video = imageInfo instanceof VideoInfo;
        this.name = title;
        this.url = video ? StringUtils.defaultString(getWebmUrlIfExists((VideoInfo) imageInfo), "")
                : StringUtils.defaultString(imageInfo.getOriginalUrl(), "");
        this.mimeType = imageInfo.getMimeType();
        this.thumbUrl = imageInfo.getThumbUrl();
        this.width = imageInfo.getWidth();
        this.height = imageInfo.getHeight();
        this.metadata = imageInfo.getMetadata();
        this.license = this.metadata != null ? new ImageLicense(this.metadata) : new ImageLicense();

        if (video) {
            this.derivatives = ((VideoInfo) imageInfo).getDerivatives();
        }
    }

    private String getWebmUrlIfExists(@NonNull VideoInfo videoInfo) {
        if (videoInfo.getDerivatives() != null) {
            for (Derivative derivative : videoInfo.getDerivatives()) {
                if (derivative.getType() != null && derivative.getType().contains("webm")) {
                    return derivative.getSrc();
                }
            }
        }
        return null;
    }

    // GalleryItem constructor for Featured Images from the feed, where we know enough to display it
    // in the gallery but don't have a lot of extra info
    GalleryItem(String name) {
        this.name = name;
        this.url = null;
        this.mimeType = "*/*";
        this.thumbUrl = null;
        this.metadata = null;
        this.width = 0;
        this.height = 0;
        this.license = new ImageLicense();
    }

    @NonNull public String getName() {
        return name;
    }

    // TODO: Convert to Uri
    @Nullable public String getUrl() {
        return url;
    }

    void setUrl(@NonNull String url) {
        this.url = url;
    }

    @NonNull public String getMimeType() {
        return mimeType;
    }

    void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    @Nullable public ExtMetadata getMetadata() {
        return metadata;
    }

    // TODO: Convert to Uri
    @Nullable public String getThumbUrl() {
        return thumbUrl;
    }

    void setThumbUrl(@NonNull String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public int getWidth() {
        return width;
    }

    void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    void setHeight(int height) {
        this.height = height;
    }

    @NonNull public ImageLicense getLicense() {
        return license;
    }

    void setLicense(@NonNull ImageLicense license) {
        this.license = license;
    }

    @NonNull public String getLicenseUrl() {
        return license.getLicenseUrl();
    }

    @Nullable public List<Derivative> getDerivatives() {
        return derivatives;
    }
}
