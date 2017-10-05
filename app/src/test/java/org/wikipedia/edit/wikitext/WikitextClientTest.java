package org.wikipedia.edit.wikitext;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class WikitextClientTest extends MockWebServerTest {
    private WikitextClient subject = new WikitextClient();
    private PageTitle title = new PageTitle(null, "TEST", WikiSite.forLanguageCode("test"));

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("wikitext.json");

        WikitextClient.Callback cb = mock(WikitextClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, "User:Mhollo/sandbox", "\\o/\n\ntest12\n\n3");
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        WikitextClient.Callback cb = mock(WikitextClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();

        WikitextClient.Callback cb = mock(WikitextClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("(-(-_(-_-)_-)-)");

        WikitextClient.Callback cb = mock(WikitextClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull WikitextClient.Callback cb,
                                       @NonNull String expectedTitle,
                                       @NonNull String expectedText) {
        verify(cb).success(eq(call), eq(expectedTitle), eq(expectedText));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull WikitextClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(String.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull WikitextClient.Callback cb) {
        return subject.request(service(WikitextClient.Service.class), title, 0, cb);
    }
}
