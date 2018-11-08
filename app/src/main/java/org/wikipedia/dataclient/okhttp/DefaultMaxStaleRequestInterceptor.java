package org.wikipedia.dataclient.okhttp;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This interceptor adds a `max-stale` parameter to the Cache-Control header that directs
 * OkHttp to return cached responses without going to the network first.
 */
class DefaultMaxStaleRequestInterceptor implements Interceptor {
    @Override public Response intercept(@NonNull Chain chain) throws IOException {
        Request req = chain.request();

        if (!req.cacheControl().noCache()) {
            // Set the max-stale parameter based on whether we're preferring offline content:
            // If we prefer offline content, then max-stale can be infinity. (OkHttp will still perform
            // a network call if the request is explicitly noCache)
            // And if we don't prefer offline content, then max-stale can be zero. (OkHttp will still
            // perform a conditional GET based on ETag or If-Modified-Since)

            int maxStaleSeconds = Prefs.preferOfflineContent() || !WikipediaApp.getInstance().isOnline() ? Integer.MAX_VALUE : 0;
            req = req.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .maxStale(maxStaleSeconds, TimeUnit.SECONDS).build())
                    .build();
        }

        return chain.proceed(req);
    }
}
