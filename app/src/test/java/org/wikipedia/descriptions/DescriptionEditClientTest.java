package org.wikipedia.descriptions;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.descriptions.DescriptionEditClient.Callback;
import org.wikipedia.descriptions.DescriptionEditClient.Service;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DescriptionEditClientTest extends MockWebServerTest {
    @NonNull private final DescriptionEditClient subject = new DescriptionEditClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("description_edit.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestAbusefilterWarning() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_warning.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackAbusefilter(call, cb, false);
    }

    @Test public void testRequestAbusefilterDisallowed() throws Throwable {
        enqueueFromFile("description_edit_abusefilter_disallowed.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackAbusefilter(call, cb, true);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueueFromFile("description_edit_unknown_site.json");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, RetrofitException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        Call<DescriptionEdit> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<DescriptionEdit> call,
                                       @NonNull Callback cb) {
        verify(cb).success(eq(call));
        //noinspection unchecked
        verify(cb, never()).abusefilter(any(Call.class), any(Boolean.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackAbusefilter(@NonNull Call<DescriptionEdit> call,
                                           @NonNull Callback cb,
                                           boolean disallowed) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class));
        verify(cb).abusefilter(eq(call), eq(disallowed));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<DescriptionEdit> call,
                                       @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class));
        //noinspection unchecked
        verify(cb, never()).abusefilter(any(Call.class), any(Boolean.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<DescriptionEdit> request(@NonNull Callback cb) {
        final PageTitle pageTitle = new PageTitle("foo", WikiSite.forLanguageCode("en"));
        return subject.request(service(Service.class), pageTitle, "some new description", "en",
                false, cb);
    }
}