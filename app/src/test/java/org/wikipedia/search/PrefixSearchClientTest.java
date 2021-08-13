package org.wikipedia.search;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;

public class PrefixSearchClientTest extends MockRetrofitTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");
    private static final int BATCH_SIZE = 20;

    private Observable<SearchResults> getObservable() {
        return getApiService().prefixSearch("foo", BATCH_SIZE, "foo")
                .map(response -> {
                    if (response != null && response.getQuery() != null && response.getQuery().getPages() != null) {
                        // noinspection ConstantConditions
                        return new SearchResults(response.getQuery().getPages(), TESTWIKI, response.getContinuation(),
                                response.getSuggestion());
                    }
                    return new SearchResults();
                });
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("prefix_search_results.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().get(0).getPageTitle().getDisplayTextValue().equals("Narthecium"));
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("prefix_search_results_empty.json");
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
                .assertError(IOException.class);
    }
}
