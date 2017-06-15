package org.wikipedia.login;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
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

public class UserIdClientTest extends MockWebServerTest {
    private UserIdClient subject = new UserIdClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_info.json");

        UserIdClient.Callback cb = mock(UserIdClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        UserIdClient.Callback cb = mock(UserIdClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();

        UserIdClient.Callback cb = mock(UserIdClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("┏━┓ ︵  /(^.^/)");

        UserIdClient.Callback cb = mock(UserIdClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull UserIdClient.Callback cb) {
        verify(cb).success(eq(call), any(Integer.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull UserIdClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(Integer.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull UserIdClient.Callback cb) {
        return subject.request(service(UserIdClient.Service.class), cb);
    }
}
