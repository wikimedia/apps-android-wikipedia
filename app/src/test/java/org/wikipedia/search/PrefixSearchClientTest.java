package org.wikipedia.search;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PrefixSearchClientTest extends MockWebServerTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");
    private final PrefixSearchClient subject = new PrefixSearchClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("prefix_search_results.json");

        PrefixSearchClient.Callback cb = mock(PrefixSearchClient.Callback.class);
        Call<PrefixSearchResponse> call = request("foo", cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("prefix_search_results_empty.json");

        PrefixSearchClient.Callback cb = mock(PrefixSearchClient.Callback.class);
        Call<PrefixSearchResponse> call = request("bar", cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        PrefixSearchClient.Callback cb = mock(PrefixSearchClient.Callback.class);
        Call<PrefixSearchResponse> call = request("foo", cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        PrefixSearchClient.Callback cb = mock(PrefixSearchClient.Callback.class);
        Call<PrefixSearchResponse> call = request("foo", cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        PrefixSearchClient.Callback cb = mock(PrefixSearchClient.Callback.class);
        Call<PrefixSearchResponse> call = request("foo", cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<PrefixSearchResponse> call,
                                       @NonNull PrefixSearchClient.Callback cb) {
        verify(cb).success(eq(call), any(SearchResults.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<PrefixSearchResponse> call,
                                       @NonNull PrefixSearchClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(SearchResults.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<PrefixSearchResponse> request(@NonNull String title, @NonNull PrefixSearchClient.Callback cb) {
        return subject.request(service(PrefixSearchClient.Service.class), TESTWIKI, title, cb);
    }
}
