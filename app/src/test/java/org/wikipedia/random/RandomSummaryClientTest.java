package org.wikipedia.random;


import android.support.annotation.NonNull;

import com.google.gson.JsonParseException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomSummaryClient.Callback;
import org.wikipedia.random.RandomSummaryClient.Service;
import org.wikipedia.test.MockWebServerTest;

import java.io.IOException;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RandomSummaryClientTest extends MockWebServerTest {
    @NonNull private RandomSummaryClient client = new RandomSummaryClient();

    @Test
    public void testRequestEligible() throws Throwable {
        enqueueFromFile("rb_page_summary_valid.json");

        Callback cb = mock(Callback.class);
        Call<RbPageSummary> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueFromFile("rb_page_summary_malformed.json");

        Callback cb = mock(Callback.class);
        Call<RbPageSummary> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, JsonParseException.class);
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();

        Callback cb = mock(Callback.class);
        Call<RbPageSummary> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, IOException.class);
    }

    @NonNull private Call<RbPageSummary> request(@NonNull Callback cb) {
        return client.request(service(Service.class), WikiSite.forLanguageCode("test"), cb);
    }

    private void assertCallbackSuccess(@NonNull Call<RbPageSummary> call,
                                       @NonNull Callback cb) {
        verify(cb).onSuccess(eq(call), any(PageTitle.class));
        //noinspection unchecked
        verify(cb, never()).onError(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<RbPageSummary> call,
                                       @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> expectedThrowable) {
        //noinspection unchecked
        verify(cb, never()).onSuccess(any(Call.class), any(PageTitle.class));
        verify(cb).onError(eq(call), isA(expectedThrowable));
    }
}
