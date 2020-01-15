package org.wikipedia.page;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class GeoUnmarshaller {
    static final String LATITUDE = "lat";
    static final String LONGITUDE = "lon";

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

    @NonNull
    public static Location unmarshal(@NonNull JSONObject jsonObj) {
        Location ret = new Location((String) null);
        ret.setLatitude(jsonObj.optDouble(LATITUDE));
        ret.setLongitude(jsonObj.optDouble(LONGITUDE));
        return ret;
    }

    private GeoUnmarshaller() { }
}
