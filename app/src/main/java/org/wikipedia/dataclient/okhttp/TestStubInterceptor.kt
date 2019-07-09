package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class TestStubInterceptor : Interceptor {
    interface Callback {
        @Throws(IOException::class)
        fun getResponse(request: Interceptor.Chain): Response
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return if (CALLBACK != null) {
            CALLBACK!!.getResponse(chain)
        } else chain.proceed(chain.request())
    }

    companion object {
        var CALLBACK: Callback? = null
    }
}
