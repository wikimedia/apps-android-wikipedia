package org.wikipedia.edit.wikitext;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class WikitextClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("wikitext.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(response -> response.query().firstPage().revisions().get(0).content().equals("\\o/\n\ntest12\n\n3")
                        && response.query().firstPage().revisions().get(0).timeStamp().equals("2018-03-18T18:10:54Z"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MwException.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getWikiTextForSection("User:Mhollo/sandbox", 0);
    }
}
