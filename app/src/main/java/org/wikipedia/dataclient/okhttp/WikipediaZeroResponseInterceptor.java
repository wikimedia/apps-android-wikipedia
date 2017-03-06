package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.wikipedia.zero.WikipediaZeroHandler;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class WikipediaZeroResponseInterceptor implements Interceptor {
    @NonNull private final WikipediaZeroHandler cb;

    public WikipediaZeroResponseInterceptor(@NonNull WikipediaZeroHandler cb) {
        this.cb = cb;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        Response rsp = chain.proceed(chain.request());
        boolean zeroConfigRequest = "zeroconfig".equals(chain.request().url().queryParameter("action"));
        if (rsp.networkResponse() != null && !zeroConfigRequest) {
            cb.onHeaderCheck(rsp.headers());
        }
        return rsp;
    }
}
