package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.util.UriUtil
import java.io.IOException

internal class TitleEncodeInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return if (chain.request().url.pathSize > 2) {
            val pathSegments = chain.request().url.pathSegments
            val builder = chain.request().url.newBuilder()
            for (i in pathSegments.indices) {
                builder.setEncodedPathSegment(i, UriUtil.encodeURL(pathSegments[i]))
            }
            chain.proceed(chain.request().newBuilder().url(builder.build()).build())
        } else {
            chain.proceed(chain.request())
        }
    }
}
