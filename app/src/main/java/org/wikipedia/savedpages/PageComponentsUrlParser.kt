package org.wikipedia.savedpages

import org.jsoup.Jsoup
import org.jsoup.select.QueryParser
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl
import org.wikipedia.util.log.L
import kotlin.streams.asSequence

object PageComponentsUrlParser {
    fun parse(html: String, site: WikiSite): Sequence<String> {
        return try {
            val link = QueryParser.parse("link")
            val script = QueryParser.parse("script")
            val document = Jsoup.parse(html)
            val css = document.stream().asSequence()
                .filter { it.`is`(link) && it.attr("rel") == "stylesheet" }
            val javascript = document.stream().asSequence()
                .filter { it.`is`(script) }

            // parsing CSS styles and JavaScript files
            (css.map { it.attr("href") } + javascript.map { it.attr("src") })
                .map { resolveProtocolRelativeUrl(site, it) }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            L.d("Parsing exception$e")
            emptySequence()
        }
    }
}
