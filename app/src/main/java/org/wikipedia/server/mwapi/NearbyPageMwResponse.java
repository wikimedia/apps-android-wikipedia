package org.wikipedia.server.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.json.annotations.Required;

import java.util.Collections;
import java.util.List;

public class NearbyPageMwResponse extends MwApiResponsePage {
    @SuppressWarnings("unused") @Nullable private List<Coordinates> coordinates;

    @Nullable public List<Coordinates> coordinates() {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        if (coordinates != null) {
            coordinates.removeAll(Collections.singleton(null));
        }
        return coordinates;
    }

    public static class Coordinates {
        // Use Double object type rather than primitive type so that the presence of the fields can
        // be checked correctly by the RequiredFieldsCheckOnReadTypeAdapter.
        @SuppressWarnings("unused") @Required @NonNull private Double lat;
        @SuppressWarnings("unused") @Required @NonNull private Double lon;

        public Coordinates(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double lat() {
            return lat;
        }
        public double lon() {
            return lon;
        }
    }
}