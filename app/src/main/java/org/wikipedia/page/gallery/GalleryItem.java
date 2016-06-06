package org.wikipedia.page.gallery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.ImageLicense;
import org.wikipedia.page.ImageLicenseFetchTask;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GalleryItem {

    private final JSONObject json;
    public JSONObject toJSON() {
        return json;
    }

    private final String name;
    public String getName() {
        return name;
    }

    private String url;
    public String getUrl() {
        return url;
    }

    private final String mimeType;
    public String getMimeType() {
        return mimeType;
    }

    private final HashMap<String, String> metadata;
    public Map<String, String> getMetadata() {
        return metadata;
    }

    private final String thumbUrl;
    public String getThumbUrl() {
        return thumbUrl;
    }

    private final int width;
    public int getWidth() {
        return width;
    }

    private final int height;
    public int getHeight() {
        return height;
    }

    @NonNull private final ImageLicense license;
    @NonNull public ImageLicense getLicense() {
        return license;
    }

    @NonNull public String getLicenseUrl() {
        return license.getLicenseUrl();
    }

    public GalleryItem(String name) {
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

    public GalleryItem(JSONObject json) throws JSONException {
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
