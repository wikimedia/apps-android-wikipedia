package org.wikipedia.search;

import org.junit.Test;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.restbase.RbRelatedPages;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public class RelatedPagesSearchClientTest extends MockRetrofitTest {
    private static final String RAW_JSON_FILE = "related_pages_search_results.json";

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithNoLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.size() == 5
                        && result.get(4).getThumbnailUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg/320px-Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg")
                        && result.get(4).getDisplayTitle().equals("Dog intelligence")
                        && result.get(4).getDescription() == null);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);
        getRestService().getRelatedPages("foo")
                .map(response -> response.getPages(3))
                .test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.size() == 3
                        && result.get(0).getThumbnailUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/European_grey_wolf_in_Prague_zoo.jpg/291px-European_grey_wolf_in_Prague_zoo.jpg")
                        && result.get(0).getDisplayTitle().equals("Wolf")
                        && result.get(0).getDescription().equals("species of mammal"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getRestService().getRelatedPages("foo")
                .map(RbRelatedPages::getPages)
                .test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    private Observable<List<PageSummary>> getObservable() {
        return getRestService().getRelatedPages("foo").map(RbRelatedPages::getPages);
    }
}
