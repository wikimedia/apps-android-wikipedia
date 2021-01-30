package org.wikipedia.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import org.apache.commons.lang3.StringUtils
import org.intellij.lang.annotations.RegExp
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L.d
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

object UriUtil {
    const val LOCAL_URL_SETTINGS = "#settings"
    const val LOCAL_URL_LOGIN = "#login"
    const val LOCAL_URL_CUSTOMIZE_FEED = "#customizefeed"
    const val LOCAL_URL_LANGUAGES = "#languages"

    const val WIKI_REGEX = "/(wiki|[a-z]{2,3}|[a-z]{2,3}-.*)/"

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent. If the string cannot be decoded, the
     * original string is returned.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    @JvmStatic
    fun decodeURL(url: String): String {
        return try {
            // Force decoding of plus sign, since the built-in decode() function will replace
            // plus sign with space.
            URLDecoder.decode(url.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            // Swallow IllegalArgumentException (can happen with malformed encoding), and just
            // return the original string.
            d("URL decoding failed. String was: $url")
            url
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun encodeURL(url: String): String {
        return try {
            // Before returning, explicitly convert plus signs to encoded spaces, since URLEncoder
            // does that for some reason.
            URLEncoder.encode(url, "UTF-8").replace("+", "%20")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Open the specified URI in an external browser (even if our app's intent filter
     * matches the given URI)
     *
     * @param context Context of the calling app
     * @param uri URI to open in an external browser
     */
    @JvmStatic
    fun visitInExternalBrowser(context: Context, uri: Uri) {
        val chooserIntent = ShareUtil.createChooserIntent(Intent(Intent.ACTION_VIEW, uri), context)
        try {
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            // This means that there was no way to handle this link.
            // We will just show a toast now. FIXME: Make this more visible?
            ShareUtil.showUnresolvableIntentMessage(context)
        }
    }

    @JvmStatic
    fun resolveProtocolRelativeUrl(wiki: WikiSite,
                                   url: String): String {
        val ret = resolveProtocolRelativeUrl(url)

        // also handle images like /w/extensions/ImageMap/desc-20.png?15600 on Estados Unidos
        // or like /api/rest_v1/page/graph/png/API/0/019dd76b5f4887040716e65de53802c5033cb40c.png
        return if (ret.startsWith("./") || ret.startsWith("/w/") || ret.startsWith("/wiki/")
                || ret.startsWith("/api/")) wiki.uri().buildUpon().appendEncodedPath(ret.replaceFirst("/".toRegex(), "")).build().toString() else ret
    }

    /**
     * Resolves a potentially protocol relative URL to a 'full' URL
     *
     * @param url Url to check for (and fix) protocol relativeness
     * @return A fully qualified, protocol specified URL
     */
    @JvmStatic
    fun resolveProtocolRelativeUrl(url: String): String {
        return if (url.startsWith("//")) WikipediaApp.getInstance().wikiSite.scheme() + ":" + url else url
    }

    @JvmStatic
    fun isValidPageLink(uri: Uri): Boolean {
        return ((!TextUtils.isEmpty(uri.authority)
                && uri.authority!!.endsWith("wikipedia.org")
                && !TextUtils.isEmpty(uri.path)
                && uri.path!!.matches(("^" + WIKI_REGEX + ".*").toRegex()))
                && (uri.fragment == null
                || (uri.fragment!!.isNotEmpty()
                && !uri.fragment!!.startsWith("cite"))))
    }

    @JvmStatic
    fun handleExternalLink(context: Context, uri: Uri) {
        visitInExternalBrowser(context, uri)
    }

    @JvmStatic
    fun getUrlWithProvenance(context: Context, title: PageTitle,
                             @StringRes provId: Int): String {
        return title.uri + "?wprov=" + context.getString(provId)
    }

    @JvmStatic
    fun getFilenameFromUploadUrl(url: String): String {
        val splitArray = url.split("/".toRegex()).toTypedArray()
        val thumbnailName = splitArray[splitArray.size - 1]
        return if (url.contains("/thumb/") && splitArray.size > 2) {
            splitArray[splitArray.size - 2]
        } else thumbnailName
    }

    /**
     * Note that while this method also replaces '_' with spaces it doesn't fully decode the string.
     */
    @JvmStatic
    fun getTitleFromUrl(url: String): String {
        return removeFragment(removeLinkPrefix(url)).replace("_", " ")
    }

    /** Get language variant code from a Uri, e.g. "zh.*", otherwise returns empty string.  */
    @JvmStatic
    fun getLanguageVariantFromUri(uri: Uri): String {
        if (TextUtils.isEmpty(uri.path)) {
            return ""
        }
        val parts = StringUtils.split(StringUtils.defaultString(uri.path), '/')
        return if (parts.size > 1 && parts[0] != "wiki") parts[0] else ""
    }

    /** For internal links only  */
    @JvmStatic
    fun removeInternalLinkPrefix(link: String): String {
        return link.replaceFirst(WIKI_REGEX, "")
    }

    /** For links that could be internal or external links  */
    @JvmStatic
    fun removeLinkPrefix(link: String): String {
        return link.replaceFirst("^.*?" + WIKI_REGEX, "")
    }

    /** Removes an optional fragment portion of a URL  */
    @JvmStatic
    @VisibleForTesting
    fun removeFragment(link: String): String {
        return link.replaceFirst("#.*$", "")
    }

    fun getFragment(link: String?): String? {
        return Uri.parse(link).fragment
    }
}