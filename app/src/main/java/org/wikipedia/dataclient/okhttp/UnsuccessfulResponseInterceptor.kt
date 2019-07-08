package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        if (rsp.isSuccessful) {
            return rsp
        }
        throw HttpStatusException(rsp)
    }
}
