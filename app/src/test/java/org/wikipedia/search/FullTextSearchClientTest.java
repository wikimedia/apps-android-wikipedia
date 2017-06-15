package org.wikipedia.search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockWebServerTest;

import java.util.Map;

import retrofit2.Call;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FullTextSearchClientTest extends MockWebServerTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");

    @NonNull private final FullTextSearchClient subject = new FullTextSearchClient();

    @Test public void testRequestSuccessNoContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(null, cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestSuccessWithContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");
        String continuationJson = "{ \"gsroffset\": 20, \"continue\": \"gsroffset||\"}";

        TypeToken<Map<String, String>> typeToken = new TypeToken<Map<String, String>>(){};
        Map<String, String> continuation = GsonUnmarshaller.unmarshal(typeToken, continuationJson);

        assertThat(continuation.get("continue"), is("gsroffset||"));
        assertThat(continuation.get("gsroffset"), is("20"));

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(continuation, cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("full_text_search_results_empty.json");

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(null, cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(null, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(null, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        FullTextSearchClient.Callback cb = mock(FullTextSearchClient.Callback.class);
        Call<MwQueryResponse> call = request(null, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull FullTextSearchClient.Callback cb) {
        verify(cb).success(eq(call), any(SearchResults.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull FullTextSearchClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(SearchResults.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Call<MwQueryResponse> request(@Nullable Map<String, String> continuation,
                                          @NonNull FullTextSearchClient.Callback cb) {
        return subject.request(service(FullTextSearchClient.Service.class), TESTWIKI, "qb",
                valOrNull(continuation, "continue"), valOrNull(continuation, "gsroffset"), 20, cb);
    }

    private String valOrNull(@Nullable Map<String, String> map, @NonNull String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }
}
