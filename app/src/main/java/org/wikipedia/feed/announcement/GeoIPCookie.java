package org.wikipedia.feed.announcement;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GeoIPCookie {

    @NonNull private final String country;
    @NonNull private final String region;
    @NonNull private final String city;
    @Nullable private final Location location;

    GeoIPCookie(@NonNull String country, @NonNull String region, @NonNull String city, @Nullable Location location) {
        this.country = country;
        this.region = region;
        this.city = city;
        this.location = location;
    }

    @NonNull
    public String country() {
        return country;
    }

    @NonNull
    public String region() {
        return region;
    }

    @NonNull
    public String city() {
        return city;
    }

    @Nullable
    public Location location() {
        return location;
    }
}
