package org.wikipedia.descriptions.centralauth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.descriptions.centralauth.CentralAuthTokenClient.Callback;
import org.wikipedia.descriptions.centralauth.CentralAuthTokenClient.Service;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CentralAuthClientTest extends MockWebServerTest {
    @NonNull private final CentralAuthTokenClient subject = new CentralAuthTokenClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("centralauth.json");

        Callback cb = mock(Callback.class);
        Call<CentralAuthToken> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, "86bd5e1b225ec3e31ec98ac0526867031d8cd4b");
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueueFromFile("centralauth_notloggedin.json");

        Callback cb = mock(Callback.class);
        Call<CentralAuthToken> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, CentralAuthTokenRetrievalFailedException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        Call<CentralAuthToken> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<CentralAuthToken> call,
                                       @NonNull Callback cb,
                                       @Nullable String expectedToken) {
        verify(cb).success(eq(call), eq(expectedToken));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<CentralAuthToken> call,
                                       @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<CentralAuthToken> request(@NonNull Callback cb) {
        return subject.request(service(Service.class), cb);
    }
}