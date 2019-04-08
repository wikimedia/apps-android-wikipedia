package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.Response;

public class TestStubInterceptor implements Interceptor {
    public interface Callback {
        Response getResponse(Chain request) throws IOException;
    }

    @Nullable private static Callback CALLBACK;

    public static void setCallback(@Nullable Callback callback) {
        CALLBACK = callback;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (CALLBACK != null) {
            return CALLBACK.getResponse(chain);
        }
        return chain.proceed(chain.request());
    }
}
