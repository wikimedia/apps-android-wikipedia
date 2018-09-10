package org.wikipedia.language;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LangLinksClientTest extends MockRetrofitTest {

    @Test
    public void testRequestSuccessHasResults() throws Throwable {
        enqueueFromFile("lang_links.json");

        getApiService().getLangLinks("foo").subscribe(mwQueryResponse -> {
            assertThat(mwQueryResponse.query().langLinks(), is(notNullValue()));
            assertThat(mwQueryResponse.query().langLinks().get(0).getDisplayText(), is("Sciëntologie"));
        }, throwable -> assertTrue(false));
    }

    @Test
    public void testRequestSuccessNoResults() throws Throwable {
        enqueueFromFile("lang_links_empty.json");

        getApiService().getLangLinks("foo")
                .subscribe(mwQueryResponse -> assertThat(mwQueryResponse.query().langLinks(), is(emptyIterable())),
                        throwable -> assertTrue(false));
    }

    @Test
    public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        getApiService().getLangLinks("foo").subscribe(mwQueryResponse -> {
            assertFalse(mwQueryResponse.success());
            assertThat(mwQueryResponse.getError().getTitle(), is("unknown_action"));
            assertThat(mwQueryResponse.query(), is(nullValue()));
        }, throwable -> assertTrue(false));
    }

    @Test
    public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("⨌⨀_⨀⨌");

        getApiService().getLangLinks("foo").subscribe(mwQueryResponse -> assertTrue(false),
                throwable -> assertTrue(throwable instanceof MalformedJsonException));
    }
}
