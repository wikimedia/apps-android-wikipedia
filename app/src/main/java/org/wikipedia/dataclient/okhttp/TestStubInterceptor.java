package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

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
