package org.wikipedia.csrf;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

public class CsrfTokenClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        String expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\";
        enqueueFromFile("csrf_token.json");

        CsrfTokenClient.INSTANCE.getToken(getWikiSite(), "csrf", getApiService()).test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> result.equals(expected));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        CsrfTokenClient.INSTANCE.getToken(getWikiSite(), "csrf", getApiService()).test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        CsrfTokenClient.INSTANCE.getToken(getWikiSite(), "csrf", getApiService()).test().await()
                .assertError(Exception.class);
    }
}
