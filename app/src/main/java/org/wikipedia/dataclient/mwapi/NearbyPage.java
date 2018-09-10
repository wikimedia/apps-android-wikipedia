package org.wikipedia.dataclient.mwapi;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.util.List;

public class NearbyPage {
    @NonNull private PageTitle title;
    @Nullable private Location location;

    /** calculated externally */
    private int distance;

    public NearbyPage(@NonNull MwQueryPage page, @NonNull WikiSite wiki) {
        title = new PageTitle(page.title(), wiki);
        title.setThumbUrl(page.thumbUrl());
        List<MwQueryPage.Coordinates> coordinates = page.coordinates();
        if (coordinates == null || coordinates.isEmpty()) {
            return;
        }
        if (coordinates.get(0).lat() != null && coordinates.get(0).lon() != null) {
            location = new Location(title.getPrefixedText());
            location.setLatitude(coordinates.get(0).lat());
            location.setLongitude(coordinates.get(0).lon());
        }
    }

    public NearbyPage(@NonNull String title, @Nullable Location location) {
        this.title = new PageTitle(title, WikipediaApp.getInstance().getWikiSite());
        this.location = location;
    }

    @NonNull public PageTitle getTitle() {
        return title;
    }

    @Nullable public Location getLocation() {
        return location;
    }

    @Override public String toString() {
        return "NearbyPage{"
                + "title='" + title + '\''
                + ", thumbUrl='" + title.getThumbUrl() + '\''
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
