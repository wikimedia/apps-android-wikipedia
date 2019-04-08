package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.dataclient.okhttp.util.HttpUrlUtil
import java.io.IOException

/**
 * This interceptor strips away the `must-revalidate` directive from the Cache-Control header,
 * since this directive prevents OkHttp from returning cached responses.  This directive makes
 * sense for a web browser, which unconditionally wants the freshest content from the network,
 * but is not necessary for our app, which needs to be more permissive with allowing cached content.
 */
internal class StripMustRevalidateResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rsp = chain.proceed(chain.request())
        val url = rsp.request().url()
        val builder = rsp.newBuilder()

        if (HttpUrlUtil.isRestBase(url) || HttpUrlUtil.isMobileView(url)) {
            val cacheControl = removeDirective(rsp.cacheControl().toString(), "must-revalidate")
            builder.header("Cache-Control", cacheControl)
        }

        // If we're saving the current response to the offline cache, then strip away the Vary header.
        if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == chain.request().header(OfflineCacheInterceptor.SAVE_HEADER)) {
            builder.removeHeader("Vary")
        }

        return builder.build()
    }

    private fun removeDirective(cacheControl: String, directive: String): String {
        return cacheControl.replace("$directive, |,? ?$directive".toRegex(), "")
    }
}
