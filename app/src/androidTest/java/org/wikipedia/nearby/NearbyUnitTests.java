package org.wikipedia.nearby;

import org.wikipedia.R;
import org.json.JSONObject;
import android.location.Location;
import android.support.annotation.StringRes;
import android.test.AndroidTestCase;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit tests for Nearby related classes. Probably should refactor this into a model class.
 */
public class NearbyUnitTests extends AndroidTestCase {
    // can't seem to suppress the checkstyle warnings for MagicNumbers. Oh well.
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final double SHORT_DISTANCE = 0.001d;
    private static final double LONGER_DISTANCE = 0.01d;
    /** dist(origin, point a) */
    private static final int A = 111319;

    private Location nextLocation;
    private List<NearbyPage> nearbyPages;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        nextLocation = new Location("current");
        nextLocation.setLatitude(0.0d);
        nextLocation.setLongitude(0.0d);
        nearbyPages = new LinkedList<>();
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"c\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 3.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"b\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 2.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"a\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 1.0}] }")));
    }

    public void testSort() throws Exception {
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertEquals("a", nearbyPages.get(0).getTitle());
        assertEquals("b", nearbyPages.get(1).getTitle());
        assertEquals("c", nearbyPages.get(2).getTitle());
    }

    public void testSortWithNullLocations() throws Exception {
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"d\" }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"e\" }")));
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertEquals("a", nearbyPages.get(0).getTitle());
        assertEquals("b", nearbyPages.get(1).getTitle());
        assertEquals("c", nearbyPages.get(2).getTitle());
        // the two null location values come last but in the same order as from the original list:
        assertEquals("d", nearbyPages.get(THREE).getTitle());
        assertEquals("e", nearbyPages.get(FOUR).getTitle());
    }

    public void testCompare() throws Exception {
        NearbyPage nullLocPage = new NearbyPage(new JSONObject("{ \"title\": \"nowhere\" }"));

        calcDistances(nearbyPages);
        nullLocPage.setDistance(getDistance(nullLocPage.getLocation()));
        assertEquals(Integer.MAX_VALUE, nullLocPage.getDistance());

        NearbyDistanceComparator comp = new NearbyDistanceComparator();
        assertEquals(A, comp.compare(nearbyPages.get(1), nearbyPages.get(2)));
        assertEquals(-1 * A, comp.compare(nearbyPages.get(2), nearbyPages.get(1)));
        assertEquals(Integer.MAX_VALUE - A, comp.compare(nullLocPage, nearbyPages.get(2)));
        assertEquals((Integer.MIN_VALUE + 1) + A, comp.compare(nearbyPages.get(2), nullLocPage)); // - (max - a)
        assertEquals(0, comp.compare(nullLocPage, nullLocPage));
    }

    public void testGetDistanceLabelSameLocation() throws Exception {
        Location locationA = new Location("current");
        locationA.setLatitude(0.0d);
        locationA.setLongitude(0.0d);
        assertEquals("0 m", getDistanceLabel(locationA));
    }

    public void testGetDistanceLabelMeters() throws Exception {
        Location locationB = new Location("b");
        locationB.setLatitude(0.0d);
        locationB.setLongitude(SHORT_DISTANCE);
        assertEquals("111 m", getDistanceLabel(locationB));
    }

    public void testGetDistanceLabelKilometers() throws Exception {
        Location locationB = new Location("b");
        locationB.setLatitude(0.0d);
        locationB.setLongitude(LONGER_DISTANCE);
        assertEquals("1.11 km", getDistanceLabel(locationB));
    }

    public void testGetDistanceLabelNull() throws Exception {
        assertEquals(" ", getDistanceLabel(null));
    }

    private class NearbyDistanceComparator implements Comparator<NearbyPage> {
        @Override
        public int compare(NearbyPage a, NearbyPage b) {
            return a.getDistance() - b.getDistance();
        }
    }

    //
    // UGLY: copy of production code
    //

    /**
     * Calculates the distances from the origin to the given pages.
     * This method should be called before sorting.
     */
    private void calcDistances(List<NearbyPage> pages) {
        for (NearbyPage page : pages) {
            page.setDistance(getDistance(page.getLocation()));
        }
    }

    private int getDistance(Location otherLocation) {
        if (otherLocation == null) {
            return Integer.MAX_VALUE;
        } else {
            return (int) nextLocation.distanceTo(otherLocation);
        }
    }

    private static final int ONE_KM = 1000;
    private static final double ONE_KM_D = 1000.0d;

    private String getDistanceLabel(Location otherLocation) {
        if (otherLocation == null) {
            return " ";
        }

        final int distance = getDistance(otherLocation);
        if (distance < ONE_KM) {
            return getString(R.string.nearby_distance_in_meters, distance);
        } else {
            return getString(R.string.nearby_distance_in_kilometers, distance / ONE_KM_D);
        }
    }

    private String getString(@StringRes int id, Object... formatArgs) {
        return getContext().getString(id, formatArgs);
    }
}
