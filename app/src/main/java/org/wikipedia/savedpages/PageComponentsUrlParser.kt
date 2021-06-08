package org.wikipedia.savedpages

import org.jsoup.Jsoup
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl
import org.wikipedia.util.log.L

object PageComponentsUrlParser {
    fun parse(html: String, site: WikiSite): List<String> {
        return try {
            val document = Jsoup.parse(html)
            val css = document.select("link[rel=stylesheet]")
            val javascript = document.select("script")

            listOf(
                css.map { it.attr("href") }, // parsing css styles
                javascript.map { it.attr("src") } // parsing javascript files
            ).flatten().filter { it.isNotEmpty() }.map { resolveProtocolRelativeUrl(site, it) }
        } catch (e: Exception) {
            L.d("Parsing exception$e")
            emptyList()
        }
    }
}
