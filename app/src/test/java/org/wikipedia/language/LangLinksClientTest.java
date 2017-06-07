package org.wikipedia.language;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LangLinksClientTest extends MockWebServerTest {
    @NonNull private final LangLinksClient subject = new LangLinksClient();

    @Test public void testRequestSuccessHasResults() throws Throwable {
        List<PageTitle> expected = getExpectedResults();
        enqueueFromFile("lang_links.json");

        LangLinksClient.Callback cb = mock(LangLinksClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        List<PageTitle> expected = new ArrayList<>();
        enqueueFromFile("lang_links_empty.json");

        LangLinksClient.Callback cb = mock(LangLinksClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        LangLinksClient.Callback cb = mock(LangLinksClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        LangLinksClient.Callback cb = mock(LangLinksClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("⨌⨀_⨀⨌");

        LangLinksClient.Callback cb = mock(LangLinksClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull LangLinksClient.Callback cb,
                                       @NonNull List<PageTitle> expected) {
        verify(cb).success(eq(call), eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull LangLinksClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(List.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private List<PageTitle> getExpectedResults() {
        List<PageTitle> result = new ArrayList<>();
        result.add(new PageTitle("Sciëntologie", WikiSite.forLanguageCode("af")));
        result.add(new PageTitle("سينتولوجيا", WikiSite.forLanguageCode("ar")));
        result.add(new PageTitle("سيينتولوجيا", WikiSite.forLanguageCode("arz")));
        return result;
    }

    private Call<MwQueryResponse> request(@NonNull LangLinksClient.Callback cb) {
        PageTitle title = new PageTitle(null, "Scientology", WikiSite.forLanguageCode("en"));
        return subject.request(service(LangLinksClient.Service.class), title, cb);
    }
}
