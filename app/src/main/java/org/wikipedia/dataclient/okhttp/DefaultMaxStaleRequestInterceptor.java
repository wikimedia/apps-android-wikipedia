package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This interceptor adds a `max-stale` parameter to the Cache-Control header that directs
 * OkHttp to return cached responses without going to the network first. This is the only way
 * we can get responses from the internal OkHttp cache without conditional network GETs.
 *
 * The problem is that when `max-stale` is set, OkHttp will *always* return responses from the
 * cache when they exist, and will never go to the network. Therefore we also need another
 * interceptor (CacheControlRequestInterceptor) to explicitly try a network request if necessary,
 * and then fall back on the default cache behavior.
 */
class DefaultMaxStaleRequestInterceptor implements Interceptor {
    @Override public Response intercept(@NonNull Chain chain) throws IOException {
        Request req = chain.request();

        int maxStaleSeconds = req.cacheControl().maxStaleSeconds() < 0
                ? Integer.MAX_VALUE
                : req.cacheControl().maxStaleSeconds();
        String cacheControl = CacheControlUtil.replaceMaxStale(req.cacheControl().toString(), maxStaleSeconds);
        req = req.newBuilder().header("Cache-Control", cacheControl).build();

        return chain.proceed(req);
    }
}
