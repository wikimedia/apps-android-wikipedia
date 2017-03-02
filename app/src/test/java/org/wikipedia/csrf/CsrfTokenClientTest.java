package org.wikipedia.csrf;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.csrf.CsrfTokenClient.Callback;
import org.wikipedia.csrf.CsrfTokenClient.Service;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CsrfTokenClientTest extends MockWebServerTest {
    @NonNull private final CsrfTokenClient subject = new CsrfTokenClient();

    @Test public void testRequestSuccess() throws Throwable {
        String expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\";
        enqueueFromFile("edittoken.json");

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse<CsrfToken>> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse<CsrfToken>> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse<CsrfToken>> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse<CsrfToken>> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse<CsrfToken>> call,
                                       @NonNull Callback cb,
                                       @NonNull String expected) {
        verify(cb).success(eq(call), eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse<CsrfToken>> call,
                                       @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse<CsrfToken>> request(@NonNull Callback cb) {
        return subject.request(service(Service.class), cb);
    }
}
