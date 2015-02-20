package org.wikipedia.page.gallery;

import org.json.JSONException;
import org.json.JSONObject;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GalleryItem {
    private final String name;
    public String getName() { return name; }

    private final String url;
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
        this.name = name;
        this.url = null;
        this.mimeType = "*/*";
        this.thumbUrl = null;
        this.metadata = null;
        this.width = 0;
        this.height = 0;
    }

    public GalleryItem(JSONObject json) throws JSONException {
        this.name = json.getString("title");
        JSONObject imageinfo = (JSONObject)json.getJSONArray("imageinfo").get(0);
        this.url = imageinfo.optString("url", "");
        this.mimeType = imageinfo.getString("mime");
        this.thumbUrl = imageinfo.optString("thumburl", "");
        this.width = imageinfo.getInt("width");
        this.height = imageinfo.getInt("height");
        metadata = new HashMap<>();
        JSONObject extmetadata = imageinfo.optJSONObject("extmetadata");
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
