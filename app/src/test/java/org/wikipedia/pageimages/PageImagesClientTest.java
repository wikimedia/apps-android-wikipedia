package org.wikipedia.pageimages;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import java.util.Arrays;
import java.util.Map;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PageImagesClientTest extends MockWebServerTest {
    private static final WikiSite WIKISITE_TEST = WikiSite.forLanguageCode("test");
    private static final PageTitle PAGE_TITLE_BIDEN = new PageTitle("Joe Biden", WIKISITE_TEST);
    private static final PageTitle PAGE_TITLE_OBAMA = new PageTitle("Barack Obama", WIKISITE_TEST);

    @NonNull private final PageImagesClient subject = new PageImagesClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("reading_list_page_info.json");

        PageImagesClient.Callback cb = mock(PageImagesClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(cb).success(eq(call), captor.capture());

        Map<PageTitle, PageImage> result = captor.getValue();
        PageImage biden = result.get(PAGE_TITLE_BIDEN);
        PageImage obama = result.get(PAGE_TITLE_OBAMA);

        assertThat(biden.getTitle().getPrefixedText(), is("Joe_Biden"));
        assertThat(biden.getImageName(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Official_portrait_of_Vice_President_Joe_Biden.jpg/255px-Official_portrait_of_Vice_President_Joe_Biden.jpg"));

        assertThat(obama.getTitle().getPrefixedText(), is("Barack_Obama"));
        assertThat(obama.getImageName(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        PageImagesClient.Callback cb = mock(PageImagesClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        PageImagesClient.Callback cb = mock(PageImagesClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        PageImagesClient.Callback cb = mock(PageImagesClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull PageImagesClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(Map.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull PageImagesClient.Callback cb) {
        return subject.request(WIKISITE_TEST, service(PageImagesClient.Service.class),
                Arrays.asList(PAGE_TITLE_BIDEN, PAGE_TITLE_OBAMA), cb);
    }

}
