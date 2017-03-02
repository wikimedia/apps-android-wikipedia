package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EditPreviewClientTest extends MockWebServerTest {
    private EditPreviewClient subject = new EditPreviewClient();
    private PageTitle title = new PageTitle(null, "TEST", WikiSite.forLanguageCode("test"));

    @Test public void testRequestSuccessHasResults() throws Throwable {
        String expected = "<div class=\"mf-section-0\" id=\"mf-section-0\"><p>\\o/\\n\\ntest12\\n\\n3</p>\n\n\n\n\n</div>";
        enqueueFromFile("edit_preview.json");

        EditPreviewClient.Callback cb = mock(EditPreviewClient.Callback.class);
        Call<EditPreview> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        EditPreviewClient.Callback cb = mock(EditPreviewClient.Callback.class);
        Call<EditPreview> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();

        EditPreviewClient.Callback cb = mock(EditPreviewClient.Callback.class);
        Call<EditPreview> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("(-(-_(-_-)_-)-)");

        EditPreviewClient.Callback cb = mock(EditPreviewClient.Callback.class);
        Call<EditPreview> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<EditPreview> call,
                                       @NonNull EditPreviewClient.Callback cb,
                                       @NonNull String expected) {
        verify(cb).success(eq(call), eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<EditPreview> call,
                                       @NonNull EditPreviewClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<EditPreview> request(@NonNull EditPreviewClient.Callback cb) {
        return subject.request(service(EditPreviewClient.Service.class), title, "wikitext of change", cb);
    }
}
