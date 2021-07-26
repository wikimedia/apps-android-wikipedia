package org.wikipedia.page

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.Namespace.Companion.fromLegacyString
import org.wikipedia.settings.SiteInfoClient.getMainPageForLang
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.StringUtil.addUnderscores
import org.wikipedia.util.StringUtil.removeNamespace
import org.wikipedia.util.StringUtil.removeUnderscores
import org.wikipedia.util.UriUtil.decodeURL
import org.wikipedia.util.UriUtil.encodeURL
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
@Parcelize
class PageTitle(
    var _namespace: String?,
    // TODO: remove this SerializedName after moving away from persisting Tabs in local prefs.
    @SerializedName("site") var wikiSite: WikiSite,
    private var _text: String = "",
    var fragment: String? = null,
    var thumbUrl: String?,
    var description: String? = null,
    // TODO: remove after the restbase endpoint supports ZH variants.
    private var _displayText: String? = null,
    var extract: String? = null
) : Parcelable {

    var text: String
        get() { return addUnderscores(_text) }
        set(value) { _text = value }

    var displayText: String
        get() { return if (_displayText.isNullOrEmpty()) removeUnderscores(prefixedText) else _displayText!! }
        set(value) { this._displayText = value }

    // TODO: find a better way to check if the namespace is a ISO Alpha2 Code (two digits country code)
    val prefixedText: String
        get() = if (namespace.isEmpty()) text else addUnderscores(namespace) + ":" + text

    val namespace: String
        get() { return _namespace.orEmpty() }

    val isFilePage: Boolean
        get() = namespace().file()

    val isSpecial: Boolean
        get() = namespace().special()

    val isMainPage: Boolean
        get() {
            val mainPageTitle = getMainPageForLang(wikiSite.languageCode)
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
        var text = title ?: getMainPageForLang(wiki.languageCode)

        // Split off any fragment (#...) from the title
        var parts = text.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        text = parts[0]
        fragment = if (parts.size > 1) {
            addUnderscores(decodeURL(parts[1]))
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
            if (listOf(*Locale.getISOLanguages()).contains(namespaceOrLanguage)) {
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
        return fromLegacyString(wikiSite, removeUnderscores(namespace))
    }

    fun getWebApiUrl(fragment: String?): String {
        return String.format(
            "%1\$s://%2\$s/w/index.php?title=%3\$s&%4\$s",
            wikiSite.scheme(),
            wikiSite.authority(),
            encodeURL(prefixedText),
            fragment
        )
    }

    fun pageTitleForTalkPage(): PageTitle {
        val talkNamespace =
            if (namespace().user() || namespace().userTalk()) UserTalkAliasData.valueFor(
                wikiSite.languageCode
            ) else TalkAliasData.valueFor(wikiSite.languageCode)
        val pageTitle = PageTitle(talkNamespace, text, wikiSite)
        pageTitle.displayText =
            "$talkNamespace:" + if (namespace.isNotEmpty() && displayText.startsWith(namespace)
            ) removeNamespace(displayText) else displayText
        pageTitle.fragment = fragment
        return pageTitle
    }

    override fun toString(): String {
        return prefixedText
    }

    private fun getUriForDomain(domain: String): String {
        return String.format(
            "%1\$s://%2\$s/%3\$s/%4\$s%5\$s",
            wikiSite.scheme(),
            domain,
            if (domain.startsWith(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE)) wikiSite.languageCode else "wiki",
            encodeURL(prefixedText),
            if (fragment != null && fragment!!.isNotEmpty()) "#" + encodeURL(fragment!!) else ""
        )
    }

    companion object {
        /**
         * Creates a new PageTitle object.
         * Use this if you want to pass in a fragment portion separately from the title.
         *
         * @param prefixedText title of the page with optional namespace prefix
         * @param fragment optional fragment portion
         * @param wiki the wiki site the page belongs to
         * @return a new PageTitle object matching the given input parameters
         */
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
