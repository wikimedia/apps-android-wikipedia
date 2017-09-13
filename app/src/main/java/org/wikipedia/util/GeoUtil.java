package org.wikipedia.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller;

public final class GeoUtil {

    public static void sendGeoIntent(@NonNull Activity activity,
                                     @NonNull Location location,
                                     @Nullable String placeName) {
        // Using geo:latitude,longitude doesn't give a point on the map, hence using query
        String geoStr = "geo:0,0?q=" + location.getLatitude()
                + "," + location.getLongitude();
        if (!TextUtils.isEmpty(placeName)) {
            geoStr += "(" + Uri.encode(placeName) + ")";
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geoStr)));
        } catch (ActivityNotFoundException e) {
            FeedbackUtil.showMessage(activity, R.string.error_no_maps_app);
        }
    }

    @Nullable public static String getGeoIPCountry() {
        try {
            return GeoIPCookieUnmarshaller.unmarshal().country();
        } catch (IllegalArgumentException e) {
            // For our purposes, don't care about malformations in the GeoIP cookie for now.
            return null;
        }
    }

    private GeoUtil() {
    }
}
