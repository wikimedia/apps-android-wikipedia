package org.wikipedia.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.TransactionTooLargeException
import androidx.annotation.StringRes
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

object UriUtil {
    const val LOCAL_URL_SETTINGS = "#settings"
    const val LOCAL_URL_LOGIN = "#login"
    const val LOCAL_URL_CUSTOMIZE_FEED = "#customizefeed"
    const val LOCAL_URL_LANGUAGES = "#languages"
    const val WIKI_REGEX = "/(wiki|[a-z]{2,3}|[a-z]{2,3}-.*)/"

    fun decodeURL(url: String): String {
        return try {
            // Force decoding of plus sign, since the built-in decode() function will replace
            // plus sign with space.
            URLDecoder.decode(url.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            // Swallow IllegalArgumentException (can happen with malformed encoding), and just
            // return the original string.
            L.d("URL decoding failed. String was: $url")
            url
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    fun encodeURL(url: String): String {
        return try {
            // Before returning, explicitly convert plus signs to encoded spaces, since URLEncoder
            // does that for some reason.
            URLEncoder.encode(url, "UTF-8").replace("+", "%20")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    fun visitInExternalBrowser(context: Context, uri: Uri) {
        try {
            val chooserIntent = ShareUtil.getIntentChooser(context, Intent(Intent.ACTION_VIEW, uri))
            if (chooserIntent != null) {
                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(chooserIntent)
            } else {
                visitInExternalBrowserExplicit(context, uri)
            }
        } catch (e: TransactionTooLargeException) {
            L.logRemoteErrorIfProd(RuntimeException("Transaction too large for external link intent."))
        } catch (e: Exception) {
            // This means that there was no way to handle this link.
            // We will just show a toast now. FIXME: Make this more visible?
            ShareUtil.showUnresolvableIntentMessage(context)
        }
    }

    private fun visitInExternalBrowserExplicit(context: Context, uri: Uri) {
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")), PackageManager.MATCH_DEFAULT_ONLY)
            .first().let {
                val componentName = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                val newIntent = Intent(Intent.ACTION_VIEW)
                newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                newIntent.data = uri
                newIntent.component = componentName
                context.startActivity(newIntent)
            }
    }

    fun resolveProtocolRelativeUrl(wiki: WikiSite, url: String): String {
        val ret = resolveProtocolRelativeUrl(url)

        // also handle images like /w/extensions/ImageMap/desc-20.png?15600 on Estados Unidos
        // or like /api/rest_v1/page/graph/png/API/0/019dd76b5f4887040716e65de53802c5033cb40c.png
        return if (ret.startsWith("./") || ret.startsWith("/w/") ||
                ret.startsWith("/wiki/") || ret.startsWith("/api/"))
            wiki.uri.buildUpon().appendEncodedPath(ret.replaceFirst("/".toRegex(), ""))
                    .build().toString()
        else ret
    }

    fun resolveProtocolRelativeUrl(url: String): String {
        return if (url.startsWith("//")) WikipediaApp.getInstance().wikiSite.scheme() + ":" + url else url
    }

    fun isValidPageLink(uri: Uri): Boolean {
        return ((!uri.authority.isNullOrEmpty() &&
                uri.authority!!.endsWith("wikipedia.org") &&
                !uri.path.isNullOrEmpty() &&
                uri.path!!.matches(("^$WIKI_REGEX.*").toRegex())) &&
                (uri.fragment == null || (uri.fragment!!.isNotEmpty() &&
                        !uri.fragment!!.startsWith("cite"))))
    }

    fun isAppSupportedLink(uri: Uri): Boolean {
        val supportedAuthority = uri.authority?.run { WikiSite.supportedAuthority(this) } == true
        return (uri.path?.run { matches(("^$WIKI_REGEX.*").toRegex()) } == true ||
                !uri.fragment.isNullOrEmpty() ||
                !uri.getQueryParameter("title").isNullOrEmpty() && !uri.getQueryParameter("diff").isNullOrEmpty()) && supportedAuthority
    }

    fun handleExternalLink(context: Context, uri: Uri) {
        visitInExternalBrowser(context, uri)
    }

    fun getUrlWithProvenance(context: Context, title: PageTitle,
                             @StringRes provId: Int): String {
        return title.uri + "?wprov=" + context.getString(provId)
    }

    fun getFilenameFromUploadUrl(url: String): String {
        val splitList = url.split("/")
        val thumbnailName = splitList.last()
        return if (url.contains("/thumb/") && splitList.size > 2) {
            splitList[splitList.size - 2]
        } else thumbnailName
    }

    fun getTitleFromUrl(url: String): String {
        return removeFragment(removeLinkPrefix(url)).replace("_", " ")
    }

    /** Get language variant code from a Uri, e.g. "zh.*", otherwise returns empty string.  */
    fun getLanguageVariantFromUri(uri: Uri): String {
        if (uri.path.isNullOrEmpty()) {
            return ""
        }
        val parts = uri.path!!.split('/')
        return if (parts.size > 1 && parts[0] != "wiki") parts[0] else ""
    }

    /** For internal links only  */
    fun removeInternalLinkPrefix(link: String): String {
        return link.replaceFirst(WIKI_REGEX.toRegex(), "")
    }

    /** For links that could be internal or external links  */
    fun removeLinkPrefix(link: String): String {
        return link.replaceFirst("^.*?$WIKI_REGEX".toRegex(), "")
    }

    /** Removes an optional fragment portion of a URL  */
    fun removeFragment(link: String): String {
        return link.replaceFirst("#.*$".toRegex(), "")
    }

    fun parseTalkTopicFromFragment(fragment: String): String {
        val index = fragment.indexOf("Z-")
        return if (index >= 0) fragment.substring(index + 2) else fragment
    }
}
