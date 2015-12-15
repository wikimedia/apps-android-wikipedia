package org.wikipedia.page;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.Site;

/**
 * Fetch license info for a single image.
 */
public class ImageLicenseFetchTask extends PageQueryTask<ImageLicense> {
    private static final String TAG = "ImageLicenseFetchTask";

    public ImageLicenseFetchTask(Api api, Site site, PageTitle title) {
        super(api, site, title);
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "imageinfo").param("iiprop", "extmetadata");
    }

    public ImageLicense processPage(int pageId, PageTitle pageTitle, JSONObject result) {
        ImageLicense license = new ImageLicense("", "", "");
        try {
            JSONObject imageInfo = (JSONObject) result.getJSONArray("imageinfo").get(0);
            parseImageLicenseMetadata(license, imageInfo.getJSONObject("extmetadata"));
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return license;
    }

    public static void parseImageLicenseMetadata(ImageLicense imageLicense, JSONObject extmetadata) {
        imageLicense.setLicense(getValueForOptionalKey(extmetadata, "License"));
        imageLicense.setLicenseShortName(getValueForOptionalKey(extmetadata, "LicenseShortName"));
        imageLicense.setLicenseUrl(getValueForOptionalKey(extmetadata, "LicenseUrl"));
    }

    private static String getValueForOptionalKey(JSONObject object, String key) {
        return object.has(key) ? object.optJSONObject(key).optString("value") : "";
    }
}
