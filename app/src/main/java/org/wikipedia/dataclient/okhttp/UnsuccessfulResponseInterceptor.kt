package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.wikipedia.analytics.eventplatform.ClientErrorEvent

class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(HttpStatusException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())

        // If the response is successful (2xx) or a redirect (3xx), then proceed.
        // ...Unless the code is 207 (multi-status), it likely means that one or more
        // analytics events were rejected. In such a case, the error is actually contained in
        // the normal response body, and should be treated as an exception.
        if ((rsp.code < 400 && rsp.code != 207) || (rsp.code == 207 && !rsp.request.url.toString().contains("/events"))) {
            return rsp
        }

        // Otherwise, treat it as an exception and throw it.
        val e = HttpStatusException(rsp)
        rsp.closeQuietly()

        // Log this error, but only if it's not a request for a 320px thumbnail, which is a known
        // rate-limiting issue in old saved articles that were saved prior to Commons switching to
        // 330px thumbnails.
        if (!rsp.request.url.toString().contains("/320px-")) {
            ClientErrorEvent().logHttpResponse(rsp)
        }

        throw e
    }
}
