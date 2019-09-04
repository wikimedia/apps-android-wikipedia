package org.wikipedia.search;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class FullTextSearchClientTest extends MockRetrofitTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");
    private static final int BATCH_SIZE = 20;

    private Observable<SearchResults> getObservable() {
        return getApiService().fullTextSearch("foo", BATCH_SIZE, null, null)
                .map(response -> {
                    if (response.success()) {
                        // noinspection ConstantConditions
                        return new SearchResults(response.query().pages(), TESTWIKI,
                                response.continuation(), null);
                    }
                    return new SearchResults();
                });
    }

    @Test public void testRequestSuccessNoContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().get(0).getPageTitle().getDisplayText().equals("IND Queens Boulevard Line"));

    }

    @Test public void testRequestSuccessWithContinuation() throws Throwable {
        enqueueFromFile("full_text_search_results.json");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getContinuation().get("continue").equals("gsroffset||")
                        && result.getContinuation().get("gsroffset").equals("20"));
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("full_text_search_results_empty.json");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().isEmpty());
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() {
        enqueue404();
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}
