package org.wikipedia.nearby;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import junit.framework.TestCase;
import org.json.JSONObject;
import android.location.Location;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit tests for Nearby related classes. Probably should refactor this into a model class.
 */
public class NearbyUnitTests extends TestCase {
    // can't seem to suppress the checkstyle warnings for MagicNumbers. Oh well.
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final double SHORT_DISTANCE = 0.001d;
    private static final double LONGER_DISTANCE = 0.01d;
    private static final int A_B_DISTANCE = 111320;

    private Location nextLocation;
    private List<NearbyPage> nearbyPages;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        nextLocation = new Location("current");
        nextLocation.setLatitude(0.0d);
        nextLocation.setLongitude(0.0d);
        nearbyPages = new LinkedList<NearbyPage>();
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"c\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 3.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"b\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 2.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"a\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 1.0}] }")));
    }

    public void testSort() throws Exception {
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertEquals("a", nearbyPages.get(0).getTitle());
        assertEquals("b", nearbyPages.get(1).getTitle());
        assertEquals("c", nearbyPages.get(2).getTitle());
    }

    public void testSortWithNullLocations() throws Exception {
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"d\" }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"e\" }")));
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
        NearbyDistanceComparator comp = new NearbyDistanceComparator();
        assertEquals(A_B_DISTANCE, comp.compare(nearbyPages.get(0), nearbyPages.get(1)));
        assertEquals(-1, comp.compare(nearbyPages.get(0), nullLocPage));
        assertEquals(1, comp.compare(nullLocPage, nearbyPages.get(0)));
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
        public int compare(NearbyPage a, NearbyPage b) {
            if (a.getLocation() == null) {
                if (b.getLocation() == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (b.getLocation() == null) {
                return -1;
            } else {
                return getDistance(a.getLocation()) - getDistance(b.getLocation());
            }
        }
    }

    private int getDistance(Location otherLocation) {
        return (int) nextLocation.distanceTo(otherLocation);
    }

    private static final int ONE_KM = 1000;
    private static final double ONE_KM_D = 1000.0d;

    private String getDistanceLabel(Location otherLocation) {
        if (otherLocation == null) {
            return " ";
        }

        final int distance = getDistance(otherLocation);
        if (distance < ONE_KM) {
            return WikipediaApp.getInstance().getString(R.string.nearby_distance_in_meters, distance);
        } else {
            return WikipediaApp.getInstance().getString(R.string.nearby_distance_in_kilometers, distance / ONE_KM_D);
        }
    }

}
