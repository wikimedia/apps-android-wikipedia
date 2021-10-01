package org.wikipedia.page

import android.content.Context
import android.net.Uri
import com.google.gson.JsonObject
import org.wikipedia.bridge.CommunicationBridge.JSEventListener
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.LinkMovementMethodExt.UrlHandlerWithText
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.util.*

abstract class LinkHandler(protected val context: Context) : JSEventListener, UrlHandlerWithText {
    abstract fun onPageLinkClicked(anchor: String, linkText: String)
    abstract fun onInternalLinkClicked(title: PageTitle)
    abstract fun onMediaLinkClicked(title: PageTitle)
    abstract fun onDiffLinkClicked(title: PageTitle, revisionId: Long)
    abstract var wikiSite: WikiSite

    // message from JS bridge:
    override fun onMessage(messageType: String, messagePayload: JsonObject?) {
        messagePayload?.let {
            onUrlClick(UriUtil.decodeURL(it["href"].asString), it["title"]?.asString, it["text"]?.asString.orEmpty())
        }
    }

    override fun onUrlClick(url: String, titleString: String?, linkText: String) {
        var href = url
        if (href.startsWith("//")) {
            // for URLs without an explicit scheme, add our default scheme explicitly.
            href = wikiSite.scheme() + ":" + href
        } else if (href.startsWith("./")) {
            href = href.replace("./", "/wiki/")
        }

        // special: returned by page-library when clicking Read More items in the footer.
        val eventLoggingParamIndex = href.indexOf("?event-logging-label")
        if (eventLoggingParamIndex > 0) {
            href = href.substring(0, eventLoggingParamIndex)
        }
        var uri = Uri.parse(href)
        uri.fragment?.run {
            if (contains("cite")) {
                onPageLinkClicked(this, linkText)
                return@onUrlClick
            }
        }
        val knownScheme = KNOWN_SCHEMES.any { href.startsWith("$it:") }
        if (!knownScheme) {
            // for URLs without a known scheme, add our default scheme explicitly.
            uri = uri.buildUpon()
                    .scheme(wikiSite.scheme())
                    .authority(wikiSite.authority())
                    .path(href)
                    .build()
        }

        // TODO: remove this after the endpoint supporting language variants
        val convertedText = UriUtil.getTitleFromUrl(href)
        var titleStr = titleString
        if (convertedText != titleStr) {
            titleStr = convertedText
        }
        var site = WikiSite(uri)
        if (site.subdomain() == wikiSite.subdomain() && site.languageCode != wikiSite.languageCode) {
            // override the languageCode from the parent WikiSite, in case it's a variant.
            site = WikiSite(uri.authority!!, wikiSite.languageCode)
        }
        L.d("Link clicked was $uri")
        val supportedAuthority = uri.authority?.run { WikiSite.supportedAuthority(this) } == true
        when {
            uri.path?.run { matches(("^${UriUtil.WIKI_REGEX}.*").toRegex()) } == true && supportedAuthority -> {
                val newTitle = if (titleStr.isNullOrEmpty()) site.titleForInternalLink(uri.path) else PageTitle.withSeparateFragment(titleStr, uri.fragment, site)
                if (newTitle.isFilePage) {
                    onMediaLinkClicked(newTitle)
                } else {
                    onInternalLinkClicked(newTitle)
                }
            }
            !uri.fragment.isNullOrEmpty() && supportedAuthority -> {
                onPageLinkClicked(uri.fragment!!, linkText)
            }
            !uri.getQueryParameter("title").isNullOrEmpty() && !uri.getQueryParameter("diff").isNullOrEmpty() && supportedAuthority -> {
                onDiffLinkClicked(PageTitle(uri.getQueryParameter("title"), site), uri.getQueryParameter("diff")!!.toLong())
            }
            else -> {
                onExternalLinkClicked(uri)
            }
        }
    }

    open fun onExternalLinkClicked(uri: Uri) {
        UriUtil.handleExternalLink(context, uri)
    }

    companion object {
        private val KNOWN_SCHEMES = listOf("http", "https", "geo", "file", "content")
    }
}
