package org.wikipedia.dataclient.okhttp

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * This interceptor adds a `max-stale` parameter to the Cache-Control header that directs
 * OkHttp to return cached responses without going to the network first.
 */
internal class DefaultMaxStaleRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()

        if (!req.cacheControl().noCache()) {
            // Set the max-stale parameter based on whether we're preferring offline content:
            // If we prefer offline content, then max-stale can be infinity. (OkHttp will still perform
            // a network call if the request is explicitly noCache)
            // And if we don't prefer offline content, then max-stale can be zero. (OkHttp will still
            // perform a conditional GET based on ETag or If-Modified-Since)

            val maxStaleSeconds = if (Prefs.preferOfflineContent() || !WikipediaApp.getInstance().isOnline) Integer.MAX_VALUE else 0
            req = req.newBuilder()
                    .cacheControl(CacheControl.Builder()
                            .maxStale(maxStaleSeconds, TimeUnit.SECONDS).build())
                    .build()
        }

        return chain.proceed(req)
    }
}
