package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheIfErrorInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        // when max-stale is set, CacheStrategy forbids a conditional network request in
        // CacheInterceptor
        if (!chain.request().cacheControl().onlyIfCached()) {
            Request req = forceNetRequest(chain.request());
            Response rsp = null;
            try {
                rsp = chain.proceed(req);
            } catch (IOException ignore) { }

            if (rsp != null && rsp.isSuccessful()) {
                return rsp;
            }
        }

        try {
            return chain.proceed(chain.request());
        } catch (IOException e) {
            throw e;
        }
    }

    @NonNull private Request forceNetRequest(@NonNull Request req) {
        String cacheControl = CacheControlUtil.forceNetRequest(req.cacheControl().toString());
        return req.newBuilder().header("Cache-Control", cacheControl).build();
    }
}
