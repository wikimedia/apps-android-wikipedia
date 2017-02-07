package org.wikipedia.dataclient.okhttp;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.wikipedia.dataclient.okhttp.CacheControlUtil.cacheControlWithDefaultMaximumStale;

/** Sets a default max-stale cache-control argument on all requests that do not specify one */
class DefaultMaxStaleInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();

        CacheControl cacheControl = cacheControlWithDefaultMaximumStale(req.cacheControl());
        req = req.newBuilder().cacheControl(cacheControl).build();

        return chain.proceed(req);
    }
}