package org.wikipedia.savedpages

import org.jsoup.Jsoup
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl
import org.wikipedia.util.log.L

internal class PageComponentsUrlParser {
    fun parse(html: String, site: WikiSite): List<String> {
        val urls = mutableListOf<String>()
        try {
            val document = Jsoup.parse(html)
            // parsing css styles
            val css = document.select("link[rel=stylesheet]")
            for (element in css) {
                val url = element.attr("href")
                if (url.isNotEmpty()) {
                    urls.add(resolveProtocolRelativeUrl(site, url))
                }
            }

            // parsing javascript files
            val javascript = document.select("script")
            for (element in javascript) {
                val url = element.attr("src")
                if (url.isNotEmpty()) {
                    urls.add(resolveProtocolRelativeUrl(site, url))
                }
            }
        } catch (e: Exception) {
            L.d("Parsing exception$e")
        }
        return urls
    }
}
