package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.wikipedia.util.log.L;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

class ResponseLoggingInterceptor implements Interceptor {
    @NonNull private volatile HttpLoggingInterceptor.Level level = HttpLoggingInterceptor.Level.NONE;

    public ResponseLoggingInterceptor setLevel(@NonNull HttpLoggingInterceptor.Level level) {
        this.level = level;
        return this;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        Response rsp = chain.proceed(chain.request());

        if (level == HttpLoggingInterceptor.Level.NONE) {
            return rsp;
        }

        StringBuilder builder = new StringBuilder(rsp.request().url().toString());
        if (rsp.networkResponse() != null) {
            builder.append(" [net]");
        }
        if (rsp.cacheResponse() != null) {
            builder.append(" [cache]");
        }
        L.v(builder.toString());

        return rsp;
    }
}
