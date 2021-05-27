package org.wikipedia.csrf;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

public class CsrfTokenClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        String expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\";
        enqueueFromFile("csrf_token.json");

        new CsrfTokenClient(wikiSite(), 1, getApiService(), "csrf").getToken().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.equals(expected));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        new CsrfTokenClient(wikiSite(), 1, getApiService(), "csrf").getToken().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        new CsrfTokenClient(wikiSite(), 1, getApiService(), "csrf").getToken().test().await()
                .assertError(Exception.class);
    }
}
