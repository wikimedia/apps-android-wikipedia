package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.wikipedia.settings.Prefs;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Since we always set a `max-stale` parameter in the Cache-Control header, it makes OkHttp
 * *always* return from the cache first, and never try going to the network first. To compensate
 * for this behavior, this interceptor explicitly tries a network request, and then falls back
 * on the default cache behavior if the network request fails.
 */
public class CacheControlRequestInterceptor implements Interceptor {
    @Override public Response intercept(@NonNull Chain chain) throws IOException {
        if (!Prefs.preferOfflineContent() || chain.request().cacheControl().noCache()) {
            Request req = chain.request().newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build();
            Response rsp = null;
            try {
                rsp = chain.proceed(req);
            } catch (IOException ignore) { }
            if (rsp != null && rsp.isSuccessful()) {
                return rsp;
            }
        }

        return chain.proceed(chain.request());
    }
}
