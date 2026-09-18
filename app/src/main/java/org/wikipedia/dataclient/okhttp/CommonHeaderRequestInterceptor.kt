package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.RestService
import java.io.IOException

internal class CommonHeaderRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val app = WikipediaApp.instance
        val url = chain.request().url.toString()
        val builder = chain.request().newBuilder()
                .header("User-Agent", app.userAgent)
                .header("X-WMF-UUID", app.appInstallID)
        if (url.contains(RestService.PAGE_HTML_ENDPOINT)) {
            builder.header("Accept", RestService.ACCEPT_HEADER_MOBILE_HTML)
        } else if (url.contains("maps.wikimedia.org")) {
            builder.header("Referer", "https://maps.wikimedia.org/")
        }
        app.oauthClient.accessToken?.let {
            builder.header("Authorization", "Bearer $it")
        }
        return chain.proceed(builder.build())
    }
}
