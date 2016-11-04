package org.wikipedia.edit.token;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.edit.token.EditTokenClient.Callback;
import org.wikipedia.edit.token.EditTokenClient.Service;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class EditTokenClientTest extends MockWebServerTest {
    @NonNull private final EditTokenClient subject = new EditTokenClient();

    @Test
    public void testRequestSuccess() throws Throwable {
        String expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\";
        enqueueFromFile("edittoken.json");

        Callback cb = mock(Callback.class);
        Call<EditToken> call = request(cb);

        server().takeRequest();
        assertCallbackSuccess(call, cb, expected);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        Callback cb = mock(Callback.class);
        Call<EditToken> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, RetrofitException.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        Callback cb = mock(Callback.class);
        Call<EditToken> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackSuccess(@NonNull Call<EditToken> call, @NonNull Callback cb,
                                       @NonNull String expected) {
        verify(cb).success(eq(call), eq(expected));
        //noinspection unchecked
        verify(cb, never()).failure(any(Call.class), any(Throwable.class));
    }

    private void assertCallbackFailure(@NonNull Call<EditToken> call, @NonNull Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(String.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<EditToken> request(@NonNull Callback cb) {
        return subject.request(service(Service.class), cb);
    }
}
