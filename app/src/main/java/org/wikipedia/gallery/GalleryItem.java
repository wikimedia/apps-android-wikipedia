package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.util.log.L;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GalleryItem {
    @NonNull private String name;
    @Nullable private String url;
    @NonNull private String mimeType;
    @Nullable private Map<String, String> metadata;
    @Nullable private String thumbUrl;
    private int width;
    private int height;
    @NonNull private ImageLicense license;
    @Nullable private List<Derivative> derivatives;

    public GalleryItem(@NonNull String title, @NonNull ImageInfo imageInfo) {
        this.name = title;
        this.url = StringUtils.defaultString(imageInfo.getOriginalUrl(), "");
        this.mimeType = imageInfo.getMimeType();
        this.thumbUrl = imageInfo.getThumbUrl();
        this.width = imageInfo.getWidth();
        this.height = imageInfo.getHeight();
        this.license = imageInfo.getMetadata() != null
                ? new ImageLicense(imageInfo.getMetadata())
                : new ImageLicense();

        try {
            this.metadata = imageInfo.getMetadata().toMap();
        } catch (IllegalAccessException e) {
            L.e(e);
        } catch (NullPointerException e) {
            // oh well
        }
    }

    public GalleryItem(@NonNull String title, @NonNull VideoInfo videoInfo) {
        this.name = title;
        this.url = StringUtils.defaultString(getWebmUrlIfExists(videoInfo), "");
        this.mimeType = videoInfo.getMimeType();
        this.thumbUrl = videoInfo.getThumbUrl();
        this.width = videoInfo.getWidth();
        this.height = videoInfo.getHeight();
        this.license = videoInfo.getMetadata() != null
                ? new ImageLicense(videoInfo.getMetadata())
                : new ImageLicense();

        try {
            this.metadata = videoInfo.getMetadata().toMap();
        } catch (IllegalAccessException e) {
            L.e(e);
        } catch (NullPointerException e) {
            // oh well
        }
        this.derivatives = videoInfo.getDerivatives();
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

    // TODO: Use an ExtMetadata object instead of a Map
    @Nullable public Map<String, String> getMetadata() {
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

    // TODO: Update consumers and remove
    @Deprecated GalleryItem(JSONObject json) throws JSONException {
        this.name = json.getString("title");
        this.metadata = new HashMap<>();
        JSONObject objinfo;
        if (json.has("imageinfo")) {
            objinfo = (JSONObject)json.getJSONArray("imageinfo").get(0);
        } else if (json.has("videoinfo")) {
            objinfo = (JSONObject)json.getJSONArray("videoinfo").get(0);
            // in the case of video, look for a list of transcodings, so that we might
            // find a WebM version, which is playable in Android.
            if (objinfo.has("derivatives")) {
                JSONArray derivativez = objinfo.getJSONArray("derivatives");
                for (int i = 0; i < derivativez.length(); i++) {
                    JSONObject derObj = derivativez.getJSONObject(i);
                    if (derObj.getString("type").contains("webm")) {
                        // that's the one!
                        this.url = derObj.getString("src");
                    }
                }
            }
        } else {
            // In certain cases, the API returns a result with a valid "title", but no
            // "imageinfo" or "videoinfo". In this case, we don't want to throw an exception, but
            // instead just set everything to zero or null, so that this item will be filtered out
            // when the GalleryCollection is constructed.
            mimeType = "*/*";
            this.license = new ImageLicense();
            return;
        }
        if (TextUtils.isEmpty(url)) {
            this.url = objinfo.optString("url", "");
        }
        mimeType = objinfo.getString("mime");
        thumbUrl = objinfo.optString("thumburl", "");
        width = objinfo.getInt("width");
        height = objinfo.getInt("height");
        JSONObject extmetadata = objinfo.optJSONObject("extmetadata");
        if (extmetadata != null) {
            Iterator<String> keys = extmetadata.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = extmetadata.getJSONObject(key).getString("value");
                metadata.put(key, value);
            }
            ExtMetadata metadataObj = GsonUnmarshaller.unmarshal(ExtMetadata.class, extmetadata.toString());
            license = new ImageLicense(metadataObj);
        } else {
            license = new ImageLicense();
        }
    }

    @VisibleForTesting public JSONObject toJSON() throws JSONException {
        return new JSONObject(GsonMarshaller.marshal(GsonUtil.getDefaultGson(), this));
    }
}
