package org.wikipedia.offline;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.offline.CompilationClient.Callback;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CompilationClientTest extends MockWebServerTest {
    @NonNull private CompilationClient subject = new CompilationClient();

    // todo: update when compilation info spec is finalized
    @Test
    public void testRequestSuccess() throws Throwable {
        server().enqueue("{ \"compilations\": [] }");
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb).success(anyListOf(Compilation.class));
        //noinspection unchecked
        verify(cb, never()).error(any(Throwable.class));
    }

    @Test
    public void testRequestMalformed() throws Throwable {
        server().enqueue("ææææææææææææææææææ");
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Compilation.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test
    public void testRequestEmpty() throws Throwable {
        enqueueEmptyJson();
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Compilation.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test
    public void testRequestNotFound() throws Throwable {
        enqueue404();
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Compilation.class));
        verify(cb).error(any(Throwable.class));
    }

    private void request(@NonNull CompilationClient.Callback cb) {
        Call<CompilationClient.CallbackAdapter.CompilationResponse> call
                = subject.request(service(CompilationClient.Service.class));
        call.enqueue(new CompilationClient.CallbackAdapter(cb));
    }
}
