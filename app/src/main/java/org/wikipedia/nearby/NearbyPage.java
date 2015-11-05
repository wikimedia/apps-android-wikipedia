package org.wikipedia.nearby;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static org.wikipedia.util.StringUtil.capitalizeFirstChar;

/**
 * Data object holding information about a nearby page.
 * The JSONObject is expected to be formatted as follows:
 *
 * <pre>
 * {@code
 *   {
 *     "pageid": 44175,
 *     "ns": 0,
 *     "title": "San Francisco",
 *     "coordinates": [{
 *     "lat": 37.7793,
 *     "lon": -122.419,
 *     "primary": "",
 *     "globe": "earth"
 *     }]
 *   }
 * }
 * </pre>
 */
public class NearbyPage {

    private String title;
    private String thumblUrl;
    private String description;
    private Location location;

    /** calculated externally */
    private int distance;

    public NearbyPage(JSONObject json) {
        try {
            title = json.getString("title");

            final JSONArray coordsArray = json.optJSONArray("coordinates");
            if (coordsArray != null && coordsArray.length() > 0) {
                JSONObject coords = coordsArray.getJSONObject(0);
                try {
                    location = new Location(title);
                    location.setLatitude(coords.getDouble("lat"));
                    location.setLongitude(coords.getDouble("lon"));
                } catch (JSONException e) {
                    // just keep at null
                }
            }

            final JSONObject thumbnail = json.optJSONObject("thumbnail");
            if (thumbnail != null) {
                final String source = thumbnail.optString("source");
                if (source != null) {
                    thumblUrl = source;
                }
            }
            final JSONObject terms = json.optJSONObject("terms");
            if (terms != null) {
                final JSONArray descArray = terms.optJSONArray("description");
                if (descArray != null) {
                    description = capitalizeFirstChar(descArray.optString(0));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getThumblUrl() {
        return thumblUrl;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "NearbyPage{"
                + "title='" + title + '\''
                + ", thumblUrl='" + thumblUrl + '\''
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
