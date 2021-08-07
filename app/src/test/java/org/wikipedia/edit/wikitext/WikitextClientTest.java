package org.wikipedia.edit.wikitext;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.rxjava3.core.Observable;

public class WikitextClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("wikitext.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> response.getQuery().getFirstPage().revisions().get(0).content().equals("\\o/\n\ntest12\n\n3")
                        && response.getQuery().getFirstPage().revisions().get(0).timeStamp().equals("2018-03-18T18:10:54Z"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(MwException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getWikiTextForSection("User:Mhollo/sandbox", 0);
    }
}
