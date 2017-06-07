package org.wikipedia.nearby;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class NearbyClientTest extends MockWebServerTest {
    @NonNull private final NearbyClient subject = new NearbyClient();

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("nearby.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestNoResults() throws Throwable {
        enqueueFromFile("nearby_empty.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        request(cb);

        server().takeRequest();

        // If no results are found we don't call success() or failure() but just do nothing;
        // here, just make sure we're not hitting an error.
        // noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    @Test public void testLocationMissingCoordsIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_coords.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<NearbyResult> captor = ArgumentCaptor.forClass(NearbyResult.class);
        verify(cb).success(eq(call), captor.capture());

        NearbyResult result = captor.getValue();
        assertThat(result.getList().size(), is(0));
    }

    @Test public void testLocationMissingLatOnlyIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_lat.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<NearbyResult> captor = ArgumentCaptor.forClass(NearbyResult.class);
        verify(cb).success(eq(call), captor.capture());

        NearbyResult result = captor.getValue();
        assertThat(result.getList().size(), is(0));
    }

    @Test public void testLocationMissingLonOnlyIsExcludedFromResults() throws Throwable {
        enqueueFromFile("nearby_missing_lon.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<NearbyResult> captor = ArgumentCaptor.forClass(NearbyResult.class);
        verify(cb).success(eq(call), captor.capture());

        NearbyResult result = captor.getValue();
        assertThat(result.getList().size(), is(0));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("(✿ ♥‿♥)");

        NearbyClient.Callback cb = mock(NearbyClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull NearbyClient.Callback cb) {
        // Location objects contained in the NearbyPage members of the Nearby results will have
        // unique timestamps assigned on creation that cause direct comparison of the NearbyResults
        // to fail.  So, let's just check to ensure that we have a valid NearbyResult.
        verify(cb).success(eq(call), any(NearbyResult.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull NearbyClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(NearbyResult.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull NearbyClient.Callback cb) {
        return subject.request(WikiSite.forLanguageCode("test"),
                service(NearbyClient.Service.class), 0, 0, 0, cb);
    }
}
