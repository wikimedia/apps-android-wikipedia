package org.wikipedia.zero;

import android.support.annotation.NonNull;

import com.google.gson.JsonParseException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.test.MockWebServerTest;
import org.wikipedia.zero.ZeroConfigClient.Callback;
import org.wikipedia.zero.ZeroConfigClient.Service;

import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ZeroConfigClientTest extends MockWebServerTest {
    @NonNull private ZeroConfigClient client = new ZeroConfigClient();
    @NonNull private static String USER_AGENT = WikipediaApp.getInstance().getUserAgent();

    @Test public void testRequestEligible() throws Throwable {
        enqueueFromFile("wikipedia_zero_test_eligible.json");

        Callback cb = mock(Callback.class);
        request(cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, USER_AGENT);

        ArgumentCaptor<ZeroConfig> captor = ArgumentCaptor.forClass(ZeroConfig.class);
        //noinspection unchecked
        verify(cb).success(any(Call.class), captor.capture());
        ZeroConfig config = captor.getValue();

        assertThat(config, isA(ZeroConfig.class));
    }

    @Test public void testRequestIneligible() throws Throwable {
        enqueueEmptyJson();

        Callback cb = mock(Callback.class);
        request(cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, USER_AGENT);

        ArgumentCaptor<ZeroConfig> captor = ArgumentCaptor.forClass(ZeroConfig.class);
        //noinspection unchecked
        verify(cb).success(any(Call.class), captor.capture());
        ZeroConfig config = captor.getValue();

        assertThat(config, is(new ZeroConfig()));
    }

    @Test public void testRequestMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        request(cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, USER_AGENT);

        //noinspection unchecked
        verify(cb).failure(any(Call.class), any(JsonParseException.class));
    }

    @Test public void testRequestApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        Callback cb = mock(Callback.class);
        request(cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, USER_AGENT);

        //noinspection unchecked
        verify(cb).failure(any(Call.class), any(MwException.class));
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();

        Callback cb = mock(Callback.class);
        Call<ZeroConfig> call = request(cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, USER_AGENT);

        assertCallbackFailure(call, cb);
    }

    @NonNull private Call<ZeroConfig> request(@NonNull Callback cb) {
        return client.request(service(Service.class), USER_AGENT, cb);
    }

    private void assertRequestIssued(@NonNull RecordedRequest req, @NonNull String userAgent) {
        assertThat(req.getPath(), containsString(encodeSpaces(userAgent)));
    }

    private void assertCallbackFailure(@NonNull Call<ZeroConfig> call, @NonNull Callback cb) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(ZeroConfig.class));
        verify(cb).failure(eq(call), any(Throwable.class));
    }

    @NonNull private String encodeSpaces(@NonNull String string) {
        return string.replace(" ", "%20");
    }
}
