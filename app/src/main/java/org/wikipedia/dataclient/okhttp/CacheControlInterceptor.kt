package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response

/**
 * This interceptor applies to RESPONSES after they are received from the network, but before
 * they are written to cache. The goal is to manipulate the headers so that the responses would
 * be readable from cache conditionally based on the app state, instead of based on what the
 * server dictates.
 */
internal class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        val builder = rsp.newBuilder()

        if (rsp.cacheControl.mustRevalidate) {
            // Override the Cache-Control header with just a max-age directive.
            // Usually the server gives us a "must-revalidate" directive, which forces us to attempt
            // revalidating from the network even when we're offline, which will cause offline access
            // of cached data to fail. This effectively strips away "must-revalidate" so that all
            // cached data will be available offline, given that we provide a "max-stale" directive
            // when requesting it.
            builder.header("Cache-Control", "max-age=" +
                    (if (rsp.cacheControl.maxAgeSeconds > 0) rsp.cacheControl.maxAgeSeconds else 0))
        }

        // If we're saving the current response to the offline cache, then strip away the Vary header.
        if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == chain.request().header(OfflineCacheInterceptor.SAVE_HEADER)) {
            builder.removeHeader("Vary")
        }

        return builder.build()
    }
}
