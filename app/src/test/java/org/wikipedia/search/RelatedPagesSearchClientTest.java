package org.wikipedia.search;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.restbase.RbRelatedPages;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.test.MockRetrofitTest;

import java.util.List;

import io.reactivex.observers.TestObserver;

public class RelatedPagesSearchClientTest extends MockRetrofitTest {
    private static final String RAW_JSON_FILE = "related_pages_search_results.json";

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithNoLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<List<RbPageSummary>> observer = new TestObserver<>();
        getRestService().getRelatedPages("foo")
                .map(RbRelatedPages::getPages)
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.size() == 5
                        && result.get(4).getThumbnailUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg/320px-Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg")
                        && result.get(4).getDisplayTitle().equals("Dog intelligence")
                        && result.get(4).getDescription() == null);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<List<RbPageSummary>> observer = new TestObserver<>();
        getRestService().getRelatedPages("foo")
                .map(response -> response.getPages(3))
                .subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.size() == 3
                        && result.get(0).getThumbnailUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/European_grey_wolf_in_Prague_zoo.jpg/291px-European_grey_wolf_in_Prague_zoo.jpg")
                        && result.get(0).getDisplayTitle().equals("Wolf")
                        && result.get(0).getDescription().equals("species of mammal"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        TestObserver<List<RbPageSummary>> observer = new TestObserver<>();
        getRestService().getRelatedPages("foo")
                .map(RbRelatedPages::getPages)
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        TestObserver<List<RbPageSummary>> observer = new TestObserver<>();
        getRestService().getRelatedPages("foo")
                .map(RbRelatedPages::getPages)
                .subscribe(observer);

        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        TestObserver<List<RbPageSummary>> observer = new TestObserver<>();
        getRestService().getRelatedPages("foo")
                .map(RbRelatedPages::getPages)
                .subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }
}
