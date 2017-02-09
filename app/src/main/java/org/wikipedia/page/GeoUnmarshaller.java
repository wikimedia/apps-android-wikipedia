package org.wikipedia.page;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class GeoUnmarshaller {
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";

    @Nullable
    public static Location unmarshal(@Nullable String json) {
        if (json == null) {
            return null;
        }

        JSONObject jsonObj;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e) {
            return null;
        }
        return unmarshal(jsonObj);
    }

    @Nullable
    public static Location unmarshal(@NonNull JSONObject jsonObj) {
        Location ret = new Location((String) null);
        ret.setLatitude(jsonObj.optDouble(LATITUDE));
        ret.setLongitude(jsonObj.optDouble(LONGITUDE));
        return ret;
    }

    private GeoUnmarshaller() { }
}
