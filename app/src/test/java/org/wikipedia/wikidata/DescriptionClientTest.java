package org.wikipedia.wikidata;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DescriptionClientTest extends MockWebServerTest {
    private static final WikiSite WIKISITE_TEST = WikiSite.forLanguageCode("test");
    private static final PageTitle PAGE_TITLE_BIDEN = new PageTitle("Joe Biden", WIKISITE_TEST);
    private static final PageTitle PAGE_TITLE_OBAMA = new PageTitle("Barack Obama", WIKISITE_TEST);

    @NonNull private final DescriptionClient subject = new DescriptionClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("reading_list_page_info.json");

        DescriptionClient.Callback cb = mock(DescriptionClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<MwQueryPage> result = captor.getValue();
        MwQueryPage biden = result.get(0);
        MwQueryPage obama = result.get(1);

        assertThat(biden.title(), is("Joe Biden"));
        assertThat(biden.description(), is("47th Vice President of the United States"));

        assertThat(obama.title(), is("Barack Obama"));
        assertThat(obama.description(), is("44th President of the United States of America"));

    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        DescriptionClient.Callback cb = mock(DescriptionClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        DescriptionClient.Callback cb = mock(DescriptionClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        DescriptionClient.Callback cb = mock(DescriptionClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull DescriptionClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(List.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull DescriptionClient.Callback cb) {
        return subject.request(service(DescriptionClient.Service.class),
                Arrays.asList(PAGE_TITLE_BIDEN, PAGE_TITLE_OBAMA), cb);
    }
}
