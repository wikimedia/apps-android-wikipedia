package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.ImageLicense;
import org.wikipedia.page.ImageLicenseFetchTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class GalleryItem {

    private JSONObject json;
    private String name;
    private String url;
    private String mimeType;
    private HashMap<String, String> metadata;
    private String thumbUrl;
    private int width;
    private int height;
    @NonNull private ImageLicense license;

    public JSONObject toJSON() {
        return json;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    void setUrl(@NonNull String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getThumbUrl() {
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

    GalleryItem(String name) {
        this.json = null;
        this.name = name;
        this.url = null;
        this.mimeType = "*/*";
        this.thumbUrl = null;
        this.metadata = null;
        this.width = 0;
        this.height = 0;
        this.license = new ImageLicense();
    }

    GalleryItem(JSONObject json) throws JSONException {
        this.json = json;
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
                JSONArray derivatives = objinfo.getJSONArray("derivatives");
                for (int i = 0; i < derivatives.length(); i++) {
                    JSONObject derObj = derivatives.getJSONObject(i);
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
            width = 0;
            height = 0;
            thumbUrl = null;
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
            license = ImageLicenseFetchTask.imageLicenseFromMetadata(extmetadata);
        } else {
            license = new ImageLicense();
        }
    }
}
