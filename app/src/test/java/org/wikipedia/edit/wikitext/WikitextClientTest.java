package org.wikipedia.edit.wikitext;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import io.reactivex.rxjava3.core.Observable;

public class WikitextClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("wikitext.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> response.getQuery().getFirstPage().getRevisions().get(0).getContent().equals("\\o/\n\ntest12\n\n3")
                        && response.getQuery().getFirstPage().getRevisions().get(0).getTimeStamp().equals("2018-03-18T18:10:54Z"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        // The constructor is invoked via reflection if the default values are not set.
        getObservable().test().await()
                .assertError(InvocationTargetException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getWikiTextForSection("User:Mhollo/sandbox", 0);
    }
}
