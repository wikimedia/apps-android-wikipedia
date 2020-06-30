package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    @Nullable private List<Derivative> derivatives;
    @Nullable private Map<String, String> captions;
    // Fields specific to video files:
    @Nullable private List<String> codecs;
    @Nullable private String name;
    @Nullable @SerializedName("short_name") private String shortName;

    @NonNull public Map<String, String> getCaptions() {
        return captions != null ? captions : Collections.emptyMap();
    }

    public void setCaptions(@NonNull Map<String, String> captions) {
        this.captions = captions;
    }

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

    public int getThumbHeight() {
        return thumbHeight;
    }

    public int getThumbWidth() {
        return thumbWidth;
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

    @NonNull public String getCommonsUrl() {
        return StringUtils.defaultString(descriptionUrl);
    }

    @Nullable public ExtMetadata getMetadata() {
        return metadata;
    }

    @NonNull public List<Derivative> getDerivatives() {
        return derivatives != null ? derivatives : Collections.emptyList();
    }

    @Nullable public Derivative getBestDerivative() {
        if (derivatives == null || derivatives.size() == 0) {
            return null;
        }
        // TODO: make this smarter.
        return derivatives.get(derivatives.size() - 1);
    }

    public static class Derivative {
        private String src;
        private String type;
        private String title;
        private String shorttitle;
        private int width;
        private int height;
        private long bandwidth;

        @NonNull public String getSrc() {
            return StringUtils.defaultString(src);
        }
    }
}
