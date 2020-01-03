package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * This intercepts the Cache-Control header that is received from the server and manipulates it
 * to work with our caching policy:  All network responses that aren't explicitly marked as no-cache
 * shall be cached for seven (7) days. This will allow the app to temporarily operate offline more
 * seamlessly by taking advantage of more cached data.
 */
internal class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        val builder = rsp.newBuilder()
        val noCache = (chain.request().header("Cache-Control") != null
                && chain.request().header("Cache-Control")!!.contains("no-cache"))

        if (!noCache) {
            //Override the Cache-Control header with a max-stale directive in order to cache all responses
            val maxStaleDays = 7
            builder.header("Cache-Control", "max-stale=" + TimeUnit.DAYS.toSeconds(maxStaleDays.toLong()))
        }

        // If we're saving the current response to the offline cache, then strip away the Vary header.
        if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == chain.request().header(OfflineCacheInterceptor.SAVE_HEADER)) {
            builder.removeHeader("Vary")
        }

        return builder.build()
    }
}