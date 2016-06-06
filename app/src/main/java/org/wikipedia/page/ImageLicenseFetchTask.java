package org.wikipedia.page;

import android.support.annotation.NonNull;
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

    @Override
    public ImageLicense processPage(int pageId, PageTitle pageTitle, JSONObject result) {
        try {
            JSONObject imageInfo = (JSONObject) result.getJSONArray("imageinfo").get(0);
            return imageLicenseFromMetadata(imageInfo.getJSONObject("extmetadata"));
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return new ImageLicense();
    }

    @NonNull
    public static ImageLicense imageLicenseFromMetadata(JSONObject extmetadata) {
        return new ImageLicense(getValueForOptionalKey(extmetadata, "License"),
                getValueForOptionalKey(extmetadata, "LicenseShortName"),
                getValueForOptionalKey(extmetadata, "LicenseUrl"));
    }

    @NonNull
    private static String getValueForOptionalKey(JSONObject object, String key) {
        return object.has(key) ? object.optJSONObject(key).optString("value") : "";
    }
}
