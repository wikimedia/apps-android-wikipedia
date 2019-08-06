package org.wikipedia.language;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class LangLinksClientTest extends MockRetrofitTest {

    @Test
    public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("lang_links.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result ->
                        result.query().langLinks().get(0).getDisplayText().equals("Sciëntologie"));
    }

    @Test
    public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("lang_links_empty.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.query().langLinks().isEmpty());
    }

    @Test
    public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test
    public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getLangLinks("foo");
    }
}
