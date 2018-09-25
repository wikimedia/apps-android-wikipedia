package org.wikipedia.random;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockRetrofitTest;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RandomSummaryClientTest extends MockRetrofitTest {

    @Test
    public void testRequestEligible() throws Throwable {
        enqueueFromFile("rb_page_summary_valid.json");

        getRestService().getRandomSummary().subscribe(summary -> {
            assertThat(summary.getDisplayTitle(), is("Fermat's Last Theorem"));
            assertThat(summary.getDescription(), is("theorem in number theory"));
        }, throwable -> assertTrue(false));
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueFromFile("rb_page_summary_malformed.json");

        getRestService().getMedia("foo").subscribe(gallery -> assertTrue(false),
                throwable -> assertTrue(throwable instanceof MalformedJsonException));
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();

        getRestService().getMedia("foo").subscribe(gallery -> assertTrue(false),
                throwable -> assertTrue(throwable instanceof HttpStatusException));
    }
}
