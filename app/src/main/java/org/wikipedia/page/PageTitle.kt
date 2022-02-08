package org.wikipedia.page

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.language.LanguageUtil
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import java.util.*

/**
 * Represents certain vital information about a page, including the title, namespace,
 * and fragment (section anchor target).  It can also contain a thumbnail URL for the
 * page, and a short description retrieved from Wikidata.
 *
 * WARNING: This class is not immutable! Specifically, the thumbnail URL and the Wikidata
 * description can be altered after construction. Therefore do NOT rely on all the fields
 * of a PageTitle to remain constant for the lifetime of the object.
 */
@Serializable
@Parcelize
data class PageTitle(
    @SerialName("namespace") private var _namespace: String? = null,
    // TODO: remove this SerialName when Tab list is no longer serialized to shared prefs.
    @SerialName("site") var wikiSite: WikiSite,
    @SerialName("text") private var _text: String = "",
    var fragment: String? = null,
    var thumbUrl: String? = null,
    var description: String? = null,
    // TODO: remove after the restbase endpoint supports ZH variants.
    @SerialName("displayText") private var _displayText: String? = null,
    var extract: String? = null
) : Parcelable {

    var text: String
        get() = StringUtil.addUnderscores(_text)
        set(value) { _text = value }

    var displayText: String
        get() = _displayText.orEmpty().ifEmpty { StringUtil.removeUnderscores(prefixedText) }
        set(value) { this._displayText = value }

    // TODO: find a better way to check if the namespace is a ISO Alpha2 Code (two digits country code)
    val prefixedText: String
        get() = if (namespace.isEmpty()) text else StringUtil.addUnderscores(namespace) + ":" + text

    var namespace: String
        get() = _namespace.orEmpty()
        set(value) { _namespace = value; _displayText = null }

    val isFilePage: Boolean
        get() = namespace().file()

    val isSpecial: Boolean
        get() = namespace().special()

    val isUserPage: Boolean
        get() = namespace().user()

    val isMainPage: Boolean
        get() {
            val mainPageTitle = SiteInfoClient.getMainPageForLang(wikiSite.languageCode)
            return mainPageTitle == displayText
        }

    val uri: String
        get() = getUriForDomain(wikiSite.authority())

    val mobileUri: String
        get() = getUriForDomain(wikiSite.authority().replace(".wikipedia.org", ".m.wikipedia.org"))

    /**
     * Notes on the `namespace` field:
     * The localised namespace of the page as a string, or null if the page is in mainspace.
     *
     * This field contains the prefix of the page's title, as opposed to the namespace ID used by
     * MediaWiki. Therefore, mainspace pages always have a null namespace, as they have no prefix,
     * and the namespace of a page will depend on the language of the wiki the user is currently
     * looking at.
     *
     * Examples:
     * * [[Manchester]] on enwiki will have a namespace of null
     * * [[Deutschland]] on dewiki will have a namespace of null
     * * [[User:Deskana]] on enwiki will have a namespace of "User"
     * * [[Utilisateur:Deskana]] on frwiki will have a namespace of "Utilisateur", even if you got
     * to the page by going to [[User:Deskana]] and having MediaWiki automatically redirect you.
     */

    constructor(namespace: String?, text: String, fragment: String?, thumbUrl: String?, wiki: WikiSite) :
            this(namespace, wiki, text, fragment, thumbUrl, null, null, null)

    constructor(text: String?, wiki: WikiSite, thumbUrl: String?, description: String?, displayText: String?) :
            this(text, wiki, thumbUrl) {
        this._displayText = displayText
        this.description = description
    }

    constructor(
        text: String?,
        wiki: WikiSite,
        thumbUrl: String?,
        description: String?,
        displayText: String?,
        extract: String?
    ) : this(text, wiki, thumbUrl, description, displayText) {
        this.extract = extract
    }

    constructor(namespace: String?, text: String, wiki: WikiSite) :
            this(namespace, text, null, null, wiki)

    @JvmOverloads
    constructor(title: String?, wiki: WikiSite, thumbUrl: String? = null) :
            this(null, wiki, title.orEmpty(), null, thumbUrl, null, null, null) {
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        var text = title.orEmpty().ifEmpty { SiteInfoClient.getMainPageForLang(wiki.languageCode) }

        // Split off any fragment (#...) from the title
        var parts = text.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        text = parts.firstOrNull().orEmpty()
        fragment = if (parts.size > 1) {
            StringUtil.addUnderscores(UriUtil.decodeURL(parts[1]))
        } else {
            null
        }

        // Remove any URL parameters (?...) from the title
        parts = text.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size > 1 && parts[1].contains("=")) {
            text = parts[0]
        }
        parts = text.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size > 1) {
            val namespaceOrLanguage = parts[0]
            if (Locale.getISOLanguages().contains(namespaceOrLanguage)) {
                _namespace = null
                wikiSite = WikiSite(wiki.authority(), namespaceOrLanguage)
                this._text = parts.copyOfRange(1, parts.size).joinToString(":")
            } else if (parts[1].isNotEmpty() && !Character.isWhitespace(parts[1][0]) && parts[1][0] != '_') {
                wikiSite = wiki
                _namespace = namespaceOrLanguage
                this._text = parts.copyOfRange(1, parts.size).joinToString(":")
            } else {
                wikiSite = wiki
                _namespace = null
                this._text = text
            }
        } else {
            wikiSite = wiki
            _namespace = null
            this._text = text
        }
        this.thumbUrl = thumbUrl
    }

    fun namespace(): Namespace {
        return Namespace.fromLegacyString(wikiSite, StringUtil.removeUnderscores(namespace))
    }

    fun getWebApiUrl(fragment: String?): String {
        return String.format(
            "%1\$s://%2\$s/w/index.php?title=%3\$s&%4\$s",
            wikiSite.scheme(),
            wikiSite.authority(),
            UriUtil.encodeURL(prefixedText),
            fragment
        )
    }

    override fun toString(): String {
        return prefixedText
    }

    private fun getUriForDomain(domain: String): String {
        return String.format(
            "%1\$s://%2\$s/%3\$s/%4\$s%5\$s",
            wikiSite.scheme(),
            domain,
            if (LanguageUtil.isChineseVariant(domain)) wikiSite.languageCode else "wiki",
            UriUtil.encodeURL(prefixedText),
            if (!fragment.isNullOrEmpty()) "#" + UriUtil.encodeURL(fragment!!) else ""
        )
    }

    companion object {
        fun withSeparateFragment(prefixedText: String, fragment: String?, wiki: WikiSite): PageTitle {
            return if (fragment.isNullOrEmpty()) {
                PageTitle(prefixedText, wiki, null)
            } else {
                // TODO: this class needs some refactoring to allow passing in a fragment
                // without having to do string manipulations.
                PageTitle("$prefixedText#$fragment", wiki, null)
            }
        }
    }
}
