package org.wikipedia.nearby;

import android.location.Location;
import android.support.annotation.StringRes;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.wikipedia.R;
import org.wikipedia.test.TestRunner;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for Nearby related classes. Probably should refactor this into a model class.
 */
@RunWith(TestRunner.class) public class NearbyUnitTest {
    // can't seem to suppress the checkstyle warnings for MagicNumbers. Oh well.
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final double SHORT_DISTANCE = 0.001d;
    private static final double LONGER_DISTANCE = 0.01d;
    /** dist(origin, point a) */
    private static final int A = 111_319;

    private Location nextLocation;
    private List<NearbyPage> nearbyPages;

    @Before
    public void setUp() throws Throwable {
        nextLocation = new Location("current");
        nextLocation.setLatitude(0.0d);
        nextLocation.setLongitude(0.0d);
        nearbyPages = new LinkedList<>();
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"c\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 3.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"b\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 2.0}] }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"a\", \"coordinates\": [{\"lat\": 0.0, \"lon\": 1.0}] }")));
    }

    @Test public void testSort() throws Throwable {
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertThat("a", is(nearbyPages.get(0).getTitle()));
        assertThat("b", is(nearbyPages.get(1).getTitle()));
        assertThat("c", is(nearbyPages.get(2).getTitle()));
    }

    @Test public void testSortWithNullLocations() throws Throwable {
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"d\" }")));
        nearbyPages.add(new NearbyPage(new JSONObject("{ \"title\": \"e\" }")));
        calcDistances(nearbyPages);
        Collections.sort(nearbyPages, new NearbyDistanceComparator());
        assertThat("a", is(nearbyPages.get(0).getTitle()));
        assertThat("b", is(nearbyPages.get(1).getTitle()));
        assertThat("c", is(nearbyPages.get(2).getTitle()));
        // the two null location values come last but in the same order as from the original list:
        assertThat("d", is(nearbyPages.get(THREE).getTitle()));
        assertThat("e", is(nearbyPages.get(FOUR).getTitle()));
    }

    @Test public void testCompare() throws Throwable {
        NearbyPage nullLocPage = new NearbyPage(new JSONObject("{ \"title\": \"nowhere\" }"));

        calcDistances(nearbyPages);
        nullLocPage.setDistance(getDistance(nullLocPage.getLocation()));
        assertThat(Integer.MAX_VALUE, is(nullLocPage.getDistance()));

        NearbyDistanceComparator comp = new NearbyDistanceComparator();
        assertThat(A, is(comp.compare(nearbyPages.get(1), nearbyPages.get(2))));
        assertThat(-1 * A, is(comp.compare(nearbyPages.get(2), nearbyPages.get(1))));
        assertThat(Integer.MAX_VALUE - A, is(comp.compare(nullLocPage, nearbyPages.get(2))));
        assertThat((Integer.MIN_VALUE + 1) + A, is(comp.compare(nearbyPages.get(2), nullLocPage))); // - (max - a)
        assertThat(0, is(comp.compare(nullLocPage, nullLocPage)));
    }

    @Test public void testGetDistanceLabelSameLocation() throws Throwable {
        Location locationA = new Location("current");
        locationA.setLatitude(0.0d);
        locationA.setLongitude(0.0d);
        assertThat("0 m", is(getDistanceLabel(locationA)));
    }

    @Test public void testGetDistanceLabelMeters() throws Throwable {
        Location locationB = new Location("b");
        locationB.setLatitude(0.0d);
        locationB.setLongitude(SHORT_DISTANCE);
        assertThat("111 m", is(getDistanceLabel(locationB)));
    }

    @Test public void testGetDistanceLabelKilometers() throws Throwable {
        Location locationB = new Location("b");
        locationB.setLatitude(0.0d);
        locationB.setLongitude(LONGER_DISTANCE);
        assertThat("1.11 km", is(getDistanceLabel(locationB)));
    }

    @Test public void testGetDistanceLabelNull() throws Throwable {
        assertThat(" ", is(getDistanceLabel(null)));
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
        return RuntimeEnvironment.application.getString(id, formatArgs);
    }
}