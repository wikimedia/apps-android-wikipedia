package org.wikipedia.readinglist;

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

import java.util.Collections;
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

public class ReadingListPageInfoClientTest extends MockWebServerTest {
    @NonNull private final ReadingListPageInfoClient subject = new ReadingListPageInfoClient();

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testRequestSuccess() throws Throwable {
        enqueueFromFile("reading_list_page_info.json");

        ReadingListPageInfoClient.Callback cb = mock(ReadingListPageInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<MwQueryPage> result = captor.getValue();
        MwQueryPage biden = result.get(0);
        MwQueryPage obama = result.get(1);

        assertThat(biden.title(), is("Joe Biden"));
        assertThat(biden.description(), is("47th Vice President of the United States"));
        assertThat(biden.thumbUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Official_portrait_of_Vice_President_Joe_Biden.jpg/255px-Official_portrait_of_Vice_President_Joe_Biden.jpg"));

        assertThat(obama.title(), is("Barack Obama"));
        assertThat(obama.description(), is("44th President of the United States of America"));
        assertThat(obama.thumbUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        ReadingListPageInfoClient.Callback cb = mock(ReadingListPageInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        ReadingListPageInfoClient.Callback cb = mock(ReadingListPageInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        ReadingListPageInfoClient.Callback cb = mock(ReadingListPageInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull ReadingListPageInfoClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(List.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull ReadingListPageInfoClient.Callback cb) {
        return subject.request(service(ReadingListPageInfoClient.Service.class),
                Collections.singletonList(new PageTitle("test", WikiSite.forLanguageCode("test"))), cb);
    }
}
