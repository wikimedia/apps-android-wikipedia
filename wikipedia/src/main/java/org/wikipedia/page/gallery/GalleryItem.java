package org.wikipedia.page.gallery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GalleryItem {
    private final JSONObject json;
    public JSONObject toJSON() { return json; }

    private final String name;
    public String getName() { return name; }

    private String url;
    public String getUrl() { return url; }

    private final String mimeType;
    public String getMimeType() { return mimeType; }

    private final HashMap<String, String> metadata;
    public Map<String, String> getMetadata() { return metadata; }

    private final String thumbUrl;
    public String getThumbUrl() { return thumbUrl; }

    private final int width;
    public int getWidth() { return width; }

    private final int height;
    public int getHeight() { return height; }

    private String licenseName;
    private String licenseUrl;
    private boolean licenseFree = true;

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public boolean isLicenseCC() {
        if (!TextUtils.isEmpty(licenseName)) {
            if (licenseName.startsWith("cc")) {
                return true;
            }
        }
        return false;
    }

    public boolean isLicensePD() {
        if (!TextUtils.isEmpty(licenseName)) {
            if (licenseName.startsWith("pd")) {
                return true;
            }
        }
        return false;
    }

    public boolean isLicenseFree() {
        return licenseFree;
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
    }

    public GalleryItem(JSONObject json) throws JSONException {
        this.json = json;
        this.name = json.getString("title");
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
            throw new JSONException("Response did not contain required info.");
        }
        if (TextUtils.isEmpty(url)) {
            this.url = objinfo.optString("url", "");
        }
        this.mimeType = objinfo.getString("mime");
        this.thumbUrl = objinfo.optString("thumburl", "");
        this.width = objinfo.getInt("width");
        this.height = objinfo.getInt("height");
        metadata = new HashMap<>();
        JSONObject extmetadata = objinfo.optJSONObject("extmetadata");
        if (extmetadata != null) {
            Iterator<String> keys = extmetadata.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = extmetadata.getJSONObject(key).getString("value");
                metadata.put(key, value);
                switch (key) {
                    case "License":
                        licenseName = value;
                        break;
                    case "LicenseUrl":
                        licenseUrl = value;
                        break;
                    case "NonFree":
                        licenseFree = !value.equals("true");
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
