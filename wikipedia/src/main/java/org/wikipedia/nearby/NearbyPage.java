package org.wikipedia.nearby;

import android.location.Location;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data object holding information about a nearby page
 */
class NearbyPage {

    private String title;
    private String thumblUrl;
    private Location location;

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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public String getThumblUrl() {
        return thumblUrl;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "NearbyPage{"
                + "title='" + title + '\''
                + ", thumblUrl='" + thumblUrl + '\''
                + ", location=" + location
                + '}';
    }
}
