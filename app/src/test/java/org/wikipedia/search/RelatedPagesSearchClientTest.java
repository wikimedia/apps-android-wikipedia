package org.wikipedia.search;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.restbase.RbRelatedPages;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.test.MockWebServerTest;

import java.util.List;

import retrofit2.Call;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RelatedPagesSearchClientTest extends MockWebServerTest {
    private static final String RAW_JSON_FILE = "related_pages_search_results.json";
    @NonNull private final RelatedPagesSearchClient subject = new RelatedPagesSearchClient();

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithNoLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        RelatedPagesSearchClient.Callback cb = mock(RelatedPagesSearchClient.Callback.class);
        Call<RbRelatedPages> call = request(-1, cb);

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<RbPageSummary> result = captor.getValue();

        assertCallbackSuccess(call, cb);
        assertThat(result != null, is(true));
        assertThat(result.size(), is(5));
        assertThat(result.get(4).getThumbnailUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg/320px-Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg"));
        assertThat(result.get(4).getDisplayTitle(), is("Dog intelligence"));
        assertThat(result.get(4).getDescription() == null, is(true));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessWithLimit() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        RelatedPagesSearchClient.Callback cb = mock(RelatedPagesSearchClient.Callback.class);
        Call<RbRelatedPages> call = request(3, cb);

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<RbPageSummary> result = captor.getValue();

        assertCallbackSuccess(call, cb);
        assertThat(result != null, is(true));
        assertThat(result.size(), is(3));
        assertThat(result.get(0).getThumbnailUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/European_grey_wolf_in_Prague_zoo.jpg/291px-European_grey_wolf_in_Prague_zoo.jpg"));
        assertThat(result.get(0).getDisplayTitle(), is("Wolf"));
        assertThat(result.get(0).getDescription(), is("species of mammal"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        RelatedPagesSearchClient.Callback cb = mock(RelatedPagesSearchClient.Callback.class);
        Call<RbRelatedPages> call = request(-1, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, Throwable.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        RelatedPagesSearchClient.Callback cb = mock(RelatedPagesSearchClient.Callback.class);
        Call<RbRelatedPages> call = request(-1, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        RelatedPagesSearchClient.Callback cb = mock(RelatedPagesSearchClient.Callback.class);
        Call<RbRelatedPages> call = request(-1, cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<RbRelatedPages> call,
                                       @NonNull RelatedPagesSearchClient.Callback cb) {
        verify(cb).success(eq(call), any(List.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<RbRelatedPages> call,
                                       @NonNull RelatedPagesSearchClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(List.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Call<RbRelatedPages> request(int limit, @NonNull RelatedPagesSearchClient.Callback cb) {
        return subject.request(service(Service.class), "test", limit, cb);
    }
}
