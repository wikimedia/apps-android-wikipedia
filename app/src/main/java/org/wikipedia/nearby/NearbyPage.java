package org.wikipedia.nearby;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.List;

public class NearbyPage {
    @SuppressWarnings("NullableProblems") @NonNull private String title;
    @Nullable private String thumbUrl;
    @Nullable private Location location;

    /** calculated externally */
    private int distance;

    public NearbyPage(@NonNull MwQueryPage page) {
        title = page.title();
        thumbUrl = page.thumbUrl();
        List<MwQueryPage.Coordinates> coordinates = page.coordinates();
        if (coordinates != null && !coordinates.isEmpty()) {
            location = new Location(title);
            location.setLatitude(page.coordinates().get(0).lat());
            location.setLongitude(page.coordinates().get(0).lon());
        }
    }

    @VisibleForTesting NearbyPage(@NonNull String title, @Nullable Location location) {
        this.title = title;
        this.location = location;
    }

    @NonNull public String getTitle() {
        return title;
    }

    @Nullable public String getThumbUrl() {
        return thumbUrl;
    }

    @Nullable public Location getLocation() {
        return location;
    }

    @Override public String toString() {
        return "NearbyPage{"
                + "title='" + title + '\''
                + ", thumbUrl='" + thumbUrl + '\''
                + ", location=" + location + '\''
                + ", distance='" + distance
                + '}';
    }

    /**
     * Returns the distance from the point where the device is.
     * Calculated later and can change. Needs to be set first by #setDistance!
     */
    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}
