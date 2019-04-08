package org.wikipedia.dataclient.okhttp

import org.wikipedia.WikipediaApp

import java.io.IOException

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

import org.wikipedia.settings.Prefs.isEventLoggingEnabled

internal class CommonHeaderRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val app = WikipediaApp.getInstance()
        val request = chain.request().newBuilder()
                .header("User-Agent", app.userAgent)
                .header(if (isEventLoggingEnabled()) "X-WMF-UUID" else "DNT",
                        if (isEventLoggingEnabled()) app.appInstallID else "1")
                .build()
        return chain.proceed(request)
    }
}
