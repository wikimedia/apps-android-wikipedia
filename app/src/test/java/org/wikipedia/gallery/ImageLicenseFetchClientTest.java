package org.wikipedia.gallery;

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

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ImageLicenseFetchClientTest extends MockWebServerTest{
    private static final WikiSite WIKISITE_TEST = WikiSite.forLanguageCode("test");
    private static final PageTitle PAGE_TITLE_MARK_SELBY =
            new PageTitle("File:Mark_Selby_at_Snooker_German_Masters_(DerHexer)_2015-02-04_02.jpg",
                          WIKISITE_TEST);

    @NonNull private final ImageLicenseFetchClient subject = new ImageLicenseFetchClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("image_license.json");

        ImageLicenseFetchClient.Callback cb = mock(ImageLicenseFetchClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        ArgumentCaptor<ImageLicense> captor = ArgumentCaptor.forClass(ImageLicense.class);
        verify(cb).success(eq(call), captor.capture());

        ImageLicense result = captor.getValue();

        assertThat(result.getLicense(), is("cc-by-sa-4.0"));
        assertThat(result.getLicenseShortName(), is("CC BY-SA 4.0"));
        assertThat(result.getLicenseUrl(), is("http://creativecommons.org/licenses/by-sa/4.0"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        ImageLicenseFetchClient.Callback cb = mock(ImageLicenseFetchClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        ImageLicenseFetchClient.Callback cb = mock(ImageLicenseFetchClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        ImageLicenseFetchClient.Callback cb = mock(ImageLicenseFetchClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull ImageLicenseFetchClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(ImageLicense.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull ImageLicenseFetchClient.Callback cb) {
        return subject.request(service(ImageLicenseFetchClient.Service.class), PAGE_TITLE_MARK_SELBY, cb);
    }
}
