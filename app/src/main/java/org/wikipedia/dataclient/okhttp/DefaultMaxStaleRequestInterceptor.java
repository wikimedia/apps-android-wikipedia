package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Sets a default max-stale cache-control argument on all requests that do not specify one */
class DefaultMaxStaleRequestInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();

        int maxStaleSeconds = req.cacheControl().maxStaleSeconds() < 0
                ? Integer.MAX_VALUE
                : req.cacheControl().maxStaleSeconds();
        String cacheControl = CacheControlUtil.replaceMaxStale(req.cacheControl().toString(), maxStaleSeconds);
        req = req.newBuilder().header("Cache-Control", cacheControl).build();

        return chain.proceed(req);
    }
}
