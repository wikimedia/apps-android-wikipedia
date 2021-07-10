package org.wikipedia.dataclient.okhttp

import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
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
import java.io.InputStream
import java.nio.charset.Charset

abstract class OkHttpWebViewClient : WebViewClientCompat() {
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
        if (request.url.toString().contains(RestService.PAGE_HTML_PREVIEW_ENDPOINT)) {
            return null
        }
        var response: WebResourceResponse
        try {
            val rsp = request(request)
            response = if (CONTENT_TYPE_OGG == rsp.header(HEADER_CONTENT_TYPE)) {
                rsp.close()
                return super.shouldInterceptRequest(view, request)
            } else {
                // noinspection ConstantConditions
                WebResourceResponse(rsp.body!!.contentType()!!.type + "/" + rsp.body!!.contentType()!!.subtype,
                    rsp.body!!.contentType()!!.charset(Charset.defaultCharset())!!.name(),
                    rsp.code,
                    rsp.message.ifBlank { "Unknown error" },
                    toMap(addResponseHeaders(rsp.headers)),
                    getInputStream(rsp))
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
        for (header in request.requestHeaders.keys) {
            if (header == "If-None-Match" || header == "If-Modified-Since") {
                // Strip away conditional headers from the request coming from the WebView, since
                // we want control of caching for ourselves (it can break OkHttp's caching internals).
                continue
            }
            request.requestHeaders[header]?.let {
                builder.header(header, it)
            }
        }
        return OkHttpConnectionFactory.client.newCall(addHeaders(request, builder).build()).execute()
    }

    private fun addHeaders(request: WebResourceRequest, builder: Request.Builder): Request.Builder {
        model.title?.let { title ->
            // TODO: Find a common way to set this header between here and RetrofitFactory.
            builder.header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(title.wikiSite))
            if (model.isInReadingList) {
                builder.header(OfflineCacheInterceptor.SAVE_HEADER, OfflineCacheInterceptor.SAVE_HEADER_SAVE)
            }
            builder.header(OfflineCacheInterceptor.LANG_HEADER, title.wikiSite.languageCode())
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

    private fun toMap(headers: Headers): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until headers.size) {
            map[headers.name(i)] = headers.value(i)
        }
        return map
    }

    private fun getInputStream(rsp: Response): InputStream? {
        return rsp.body?.let {
            var inputStream = it.byteStream()
            if (CONTENT_TYPE_OGG == rsp.header(HEADER_CONTENT_TYPE)) {
                inputStream = AvailableInputStream(it.byteStream(), it.contentLength())
            }
            inputStream
        }
    }

    companion object {
        private const val HEADER_CONTENT_TYPE = "content-type"
        private const val CONTENT_TYPE_OGG = "application/ogg"
        private val SUPPORTED_SCHEMES = listOf("http", "https")
    }
}
