package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import java.io.IOException

/**
 * This interceptor adds a `max-stale` parameter to the Cache-Control header that directs
 * OkHttp to return cached responses without going to the network first.
 */
internal class DefaultMaxStaleRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()

        if (!req.cacheControl.noCache &&
                req.cacheControl.maxAgeSeconds != 0 &&
                (Prefs.preferOfflineContent() || !WikipediaApp.instance.isOnline)) {
            // If we're offline, or if we prefer offline content, then raise the max-stale value
            // to infinity, since we would rather show some content than none.
            req = req.newBuilder()
                    .cacheControl(OkHttpConnectionFactory.CACHE_CONTROL_MAX_STALE)
                    .build()
        }

        return chain.proceed(req)
    }
}
