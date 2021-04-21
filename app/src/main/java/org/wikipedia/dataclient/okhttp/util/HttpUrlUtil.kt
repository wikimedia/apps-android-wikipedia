package org.wikipedia.dataclient.okhttp.util

import okhttp3.HttpUrl
import java.util.Collections

object HttpUrlUtil {
    private val RESTBASE_SEGMENT_IDENTIFIERS = listOf("rest_v1", "v1")

    @JvmStatic
    fun isRestBase(url: HttpUrl): Boolean {
        return !Collections.disjoint(url.encodedPathSegments, RESTBASE_SEGMENT_IDENTIFIERS)
    }

    @JvmStatic
    fun isMobileView(url: HttpUrl): Boolean {
        return "mobileview" == url.queryParameter("action")
    }
}
