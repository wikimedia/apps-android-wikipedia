package org.wikipedia.login;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockWebServerTest;

import java.util.Set;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UserExtendedInfoClientTest extends MockWebServerTest {
    private UserExtendedInfoClient subject = new UserExtendedInfoClient();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("user_extended_info.json");

        UserExtendedInfoClient.Callback cb = mock(UserExtendedInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        UserExtendedInfoClient.Callback cb = mock(UserExtendedInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, LoginClient.LoginFailedException.class);
    }

    @Test public void testRequestResponse404() throws Throwable {
        enqueue404();

        UserExtendedInfoClient.Callback cb = mock(UserExtendedInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("┏━┓ ︵  /(^.^/)");

        UserExtendedInfoClient.Callback cb = mock(UserExtendedInfoClient.Callback.class);
        Call<MwQueryResponse> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<MwQueryResponse> call,
                                       @NonNull UserExtendedInfoClient.Callback cb) {
        verify(cb).success(eq(call), any(Integer.class), any(Set.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull UserExtendedInfoClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(Integer.class), any(Set.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull UserExtendedInfoClient.Callback cb) {
        return subject.request(service(UserExtendedInfoClient.Service.class), "USER", cb);
    }
}
