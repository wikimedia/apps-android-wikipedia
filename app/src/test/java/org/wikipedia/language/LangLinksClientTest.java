package org.wikipedia.language;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.rxjava3.core.Observable;

public class LangLinksClientTest extends MockRetrofitTest {

    @Test
    public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("lang_links.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result ->
                        result.getQuery().getLangLinks().get(0).getDisplayText().equals("Sciëntologie"));
    }

    @Test
    public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("lang_links_empty.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getQuery().getLangLinks().isEmpty());
    }

    @Test
    public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test
    public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getLangLinks("foo");
    }
}
