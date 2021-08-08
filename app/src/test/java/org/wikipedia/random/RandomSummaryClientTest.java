package org.wikipedia.random;

import org.junit.Test;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;

public class RandomSummaryClientTest extends MockRetrofitTest {

    @Test
    public void testRequestEligible() throws Throwable {
        enqueueFromFile("rb_page_summary_valid.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(summary -> summary != null
                        && summary.getDisplayTitle().equals("Fermat's Last Theorem")
                        && summary.getDescription().equals("theorem in number theory"));
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    private Observable<PageSummary> getObservable() {
        return getRestService().getRandomSummary();
    }
}
