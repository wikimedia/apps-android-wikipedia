package org.wikipedia.search;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.rxjava3.core.Observable;

public class FullTextSearchClientTest extends MockRetrofitTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");
    private static final int BATCH_SIZE = 20;

    private Observable<SearchResults> getObservable() {
        return getApiService().fullTextSearch("foo", BATCH_SIZE, null, null)
                .map(response -> {
                    if (response.getQuery() != null) {
                        // noinspection ConstantConditions
                        return new SearchResults(response.getQuery().getPages(), TESTWIKI,
                                response.getContinuation(), null);
                    }
                    return new SearchResults();
                });
    }

    @Test public void testRequestSuccessNoContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().get(0).getPageTitle().getDisplayText().equals("IND Queens Boulevard Line"));

    }

    @Test public void testRequestSuccessWithContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getContinuation().getContinuation().equals("gsroffset||")
                        && result.getContinuation().getGsroffset() == 20);
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("full_text_search_results_empty.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().isEmpty());
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }
}
