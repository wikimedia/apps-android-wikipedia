package org.wikipedia.page;

import android.location.Location;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class GeoMarshaller {
    @Nullable
    public static String marshal(@Nullable Location object) {
        if (object == null) {
            return null;
        }

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(GeoUnmarshaller.LATITUDE, object.getLatitude());
            jsonObj.put(GeoUnmarshaller.LONGITUDE, object.getLongitude());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObj.toString();
    }

    private GeoMarshaller() { }
}
