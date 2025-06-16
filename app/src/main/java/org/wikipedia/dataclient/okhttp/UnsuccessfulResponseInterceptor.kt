package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly

class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(HttpStatusException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())

        // If the response is successful (2xx) or a redirect (3xx), then proceed.
        if (rsp.code < 400) {
            return rsp
        }

        // Otherwise, treat it as an exception and throw it.
        val e = HttpStatusException(rsp)
        rsp.closeQuietly()
        throw e
    }
}
