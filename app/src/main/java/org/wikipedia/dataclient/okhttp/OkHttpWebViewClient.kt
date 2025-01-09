package org.wikipedia.dataclient.okhttp

import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.RestService
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageViewModel
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.io.IOException
import java.nio.charset.Charset

abstract class OkHttpWebViewClient : WebViewClient() {
    /*
        Note: Any data transformations performed here are only for the benefit of WebViews.
        They should not be made into general Interceptors.
     */

    abstract val model: PageViewModel
    abstract val linkHandler: LinkHandler

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (model.shouldLoadAsMobileWeb) {
            // If the page was loaded as Mobile Web, then pass all link clicks through
            // to our own link handler.
            linkHandler.onUrlClick(UriUtil.decodeURL(url), null, "")
            return true
        }
        return false
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!SUPPORTED_SCHEMES.contains(request.url.scheme)) {
            return null
        }
        if (request.method == "POST" ||
            request.url.toString().contains(RestService.PAGE_HTML_PREVIEW_ENDPOINT) ||
            request.requestHeaders.containsKey("Range") || request.requestHeaders.containsKey("range")) {
            // We do NOT want to intercept requests coming from the WebView in the following cases:
            // 1. POST requests, because the WebView doesn't provide us the Body of the request,
            //    for security (?) reasons.
            // 2. Page previews, because we're not interested in saving or caching them.
            // 3. Requests with any kind of "Range" header, since this is very likely a request for
            //    playback of an audio or video file, which is problematic for us to intercept in
            //    the way that we do, and can lead to unintended behavior.
            return null
        }
        var response: WebResourceResponse
        try {
            val shouldLogLatency = request.url.encodedPath?.contains(RestService.PAGE_HTML_ENDPOINT) == true
            if (shouldLogLatency) {
                WikipediaApp.instance.appSessionEvent.pageFetchStart()
            }
            val rsp = request(request)
            if (rsp.networkResponse != null && shouldLogLatency) {
                WikipediaApp.instance.appSessionEvent.pageFetchEnd()
            }
            val contentType = rsp.header(HEADER_CONTENT_TYPE).orEmpty()
            response = if (contentType.startsWith("audio") || contentType.startsWith("video")) {
                // One last check to make sure we're not intercepting a media file (see comments above).
                rsp.close()
                return null
            } else {
                // noinspection ConstantConditions
                WebResourceResponse(rsp.body!!.contentType()!!.type + "/" + rsp.body!!.contentType()!!.subtype,
                    rsp.body!!.contentType()!!.charset(Charset.defaultCharset())!!.name(),
                    rsp.code,
                    rsp.message.ifBlank { "Unknown error" },
                    addResponseHeaders(rsp.headers).toMap(),
                    rsp.body?.byteStream())
            }
        } catch (e: Exception) {
            val reasonCode = if (e.message.isNullOrEmpty()) "Unknown error" else UriUtil.encodeURL(e.message!!)
            response = if (e is HttpStatusException) {
                WebResourceResponse(null, null, e.code, reasonCode, null, null)
            } else {
                WebResourceResponse(null, null, 500, reasonCode, null, null)
            }
            L.e(e)
        }
        return response
    }

    override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
        return event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_F ||
                !event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_F3
    }

    @Throws(IOException::class)
    private fun request(request: WebResourceRequest): Response {
        val builder = Request.Builder().url(request.url.toString()).cacheControl(model.cacheControl)
        for ((header, value) in request.requestHeaders) {
            if (header == "If-None-Match" || header == "If-Modified-Since") {
                // Strip away conditional headers from the request coming from the WebView, since
                // we want control of caching for ourselves (it can break OkHttp's caching internals).
                continue
            }
            value?.let {
                builder.header(header, it)
            }
        }
        return OkHttpConnectionFactory.client.newCall(addHeaders(request, builder).build()).execute()
    }

    private fun addHeaders(request: WebResourceRequest, builder: Request.Builder): Request.Builder {
        model.title?.let { title ->
            // TODO: Find a common way to set this header between here and RetrofitFactory.
            builder.header("Accept-Language", WikipediaApp.instance.getAcceptLanguage(title.wikiSite, false))
            if (model.isInReadingList) {
                builder.header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            }
            builder.header(OfflineCacheInterceptor.LANG_HEADER, title.wikiSite.languageCode)
            builder.header(OfflineCacheInterceptor.TITLE_HEADER, UriUtil.encodeURL(title.prefixedText))
            model.curEntry?.referrer?.let { referrer ->
                if (referrer.isNotEmpty()) {
                    builder.header("Referer", referrer)
                }
            }
            request.url.path?.let {
                if (it.contains(RestService.PAGE_HTML_ENDPOINT)) {
                    builder.header("X-Analytics", "pageview=1")
                }
            }
        }
        return builder
    }

    private fun addResponseHeaders(headers: Headers): Headers {
        // add CORS header to allow requests from all domains.
        return headers.newBuilder().set("Access-Control-Allow-Origin", "*").build()
    }

    companion object {
        private const val HEADER_CONTENT_TYPE = "content-type"
        private val SUPPORTED_SCHEMES = listOf("http", "https")
    }
}
