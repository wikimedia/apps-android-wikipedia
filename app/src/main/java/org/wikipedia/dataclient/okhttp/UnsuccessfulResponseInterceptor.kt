package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly

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
        throw e
    }
}
