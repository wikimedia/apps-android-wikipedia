package org.wikipedia.pageimages;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

public class PageImagesClientTest extends MockRetrofitTest {
    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("reading_list_page_info.json");

        getApiService().getPageImages("foo").test().await()
                .assertComplete().assertNoErrors()
                .assertValue(result -> {
                    return result.query().pages().get(0).title().equals("Joe Biden")
                            && result.query().pages().get(0).thumbUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Official_portrait_of_Vice_President_Joe_Biden.jpg/255px-Official_portrait_of_Vice_President_Joe_Biden.jpg")
                            && result.query().pages().get(0).title().equals("Barack Obama")
                            && result.query().pages().get(0).thumbUrl().equals("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg");
                });
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        getApiService().getPageImages("foo").test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getApiService().getPageImages("foo").test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getApiService().getPageImages("foo").test().await()
                .assertError(MalformedJsonException.class);
    }
}
