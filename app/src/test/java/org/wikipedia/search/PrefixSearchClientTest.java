package org.wikipedia.search;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.test.MockRetrofitTest;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class PrefixSearchClientTest extends MockRetrofitTest {
    private static final WikiSite TESTWIKI = new WikiSite("test.wikimedia.org");
    private static final int BATCH_SIZE = 20;

    private Observable<SearchResults> getObservable() {
        return getApiService().prefixSearch("foo", BATCH_SIZE, "foo")
                .map(response -> {
                    if (response != null && response.success() && response.query().pages() != null) {
                        // noinspection ConstantConditions
                        return new SearchResults(response.query().pages(), TESTWIKI, response.continuation(),
                                response.suggestion());
                    }
                    return new SearchResults();
                });
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("prefix_search_results.json");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.getResults().get(0).getPageTitle().getDisplayText().equals("Narthecium"));
    }

    @Test public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("prefix_search_results_empty.json");
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

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");
        TestObserver<SearchResults> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}
