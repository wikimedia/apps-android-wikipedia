package org.wikipedia.feed.announcement;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;

/*
This currently supports the "v4" version of the GeoIP cookie.
For some info about the format and contents of the cookie:
https://phabricator.wikimedia.org/diffusion/ECNO/browse/master/resources/subscribing/ext.centralNotice.geoIP.js
 */
public final class GeoIPCookieUnmarshaller {
    private static final String COOKIE_NAME = "GeoIP";

    private enum Component {
        COUNTRY, REGION, CITY, LATITUDE, LONGITUDE, VERSION
    }

    @NonNull
    public static GeoIPCookie unmarshal() {
        return unmarshal(SharedPreferenceCookieManager.getInstance().getCookieByName(COOKIE_NAME));
    }

    @VisibleForTesting
    @NonNull
    static GeoIPCookie unmarshal(@Nullable String cookie) throws IllegalArgumentException {
        if (TextUtils.isEmpty(cookie)) {
            throw new IllegalArgumentException("Cookie is empty.");
        }
        String[] components = cookie.split(":");
        if (components.length < Component.values().length) {
            throw new IllegalArgumentException("Cookie is malformed.");
        } else if (!components[Component.VERSION.ordinal()].equals("v4")) {
            throw new IllegalArgumentException("Incorrect cookie version.");
        }
        Location location = null;
        if (!TextUtils.isEmpty(components[Component.LATITUDE.ordinal()])
                && !TextUtils.isEmpty(components[Component.LONGITUDE.ordinal()])) {
            location = new Location("");
            try {
                location.setLatitude(Double.parseDouble(components[Component.LATITUDE.ordinal()]));
                location.setLongitude(Double.parseDouble(components[Component.LONGITUDE.ordinal()]));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Location is malformed.");
            }
        }
        return new GeoIPCookie(components[Component.COUNTRY.ordinal()],
                components[Component.REGION.ordinal()],
                components[Component.CITY.ordinal()],
                location);
    }

    private GeoIPCookieUnmarshaller() {
    }
}
