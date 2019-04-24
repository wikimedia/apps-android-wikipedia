package org.wikipedia.nearby;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.NearbyPage;
import org.wikipedia.test.MockRetrofitTest;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;

@SuppressWarnings("checkstyle:magicnumber")
public class NearbyClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("nearby.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(nearbyPages -> nearbyPages.get(0).getTitle().getDisplayText().equals("Bean Hollow State Beach")
                        && nearbyPages.get(0).getLocation().getLatitude() == 37.22583333
                        && nearbyPages.get(0).getLocation().getLongitude() == -122.40888889);
    }

    @Test public void testRequestNoResults() throws Throwable {
        enqueueFromFile("nearby_empty.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getApiService().nearbySearch("0|0", 1)
                .map((Function<MwQueryResponse, List<NearbyPage>>) response
                        -> response.query() != null ? response.query().nearbyPages(WikiSite.forLanguageCode("en")) : Collections.emptyList())
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(List::isEmpty);
    }

    @Test public void testLocationMissingCoordsIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_coords.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(List::isEmpty);
    }

    @Test public void testLocationMissingLatOnlyIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_lat.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(List::isEmpty);
    }

    @Test public void testLocationMissingLonOnlyIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_lon.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(List::isEmpty);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MwException.class);
    }

    @Test public void testRequestResponseFailure() {
        enqueue404();
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<List<NearbyPage>> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    private Observable<List<NearbyPage>> getObservable() {
        return getApiService().nearbySearch("0|0", 1)
                .map(response -> response.query().nearbyPages(WikiSite.forLanguageCode("en")));
    }
}
