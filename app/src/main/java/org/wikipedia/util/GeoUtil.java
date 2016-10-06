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

public final class GeoUtil {

    public static void sendGeoIntent(@NonNull Activity activity,
                                     @NonNull Location location,
                                     @Nullable String placeName) {
        String geoStr = "geo:";
        geoStr += Double.toString(location.getLatitude()) + ","
                + Double.toString(location.getLongitude());
        if (!TextUtils.isEmpty(placeName)) {
            geoStr += "?q=" + Uri.encode(placeName);
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geoStr)));
        } catch (ActivityNotFoundException e) {
            FeedbackUtil.showMessage(activity, R.string.error_no_maps_app);
        }
    }

    private GeoUtil() {
    }
}
