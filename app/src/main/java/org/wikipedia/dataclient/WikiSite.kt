package org.wikipedia.dataclient

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.UriSerializer
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.language.LanguageUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil

/**
 * The base URL and Wikipedia language code for a MediaWiki site. Examples:
 *
 *
 * <lh>Name: scheme / authority / language code</lh>
 *  * English Wikipedia: HTTPS / en.wikipedia.org / en
 *  * Chinese Wikipedia: HTTPS / zh.wikipedia.org / zh-hans or zh-hant
 *  * Meta-Wiki: HTTPS / meta.wikimedia.org / (none)
 *  * Test Wikipedia: HTTPS / test.wikipedia.org / test
 *  * Võro Wikipedia: HTTPS / fiu-vro.wikipedia.org / fiu-vro
 *  * Simple English Wikipedia: HTTPS / simple.wikipedia.org / simple
 *  * Simple English Wikipedia (beta cluster mirror): HTTP / simple.wikipedia.beta.wmflabs.org / simple
 *  * Development: HTTP / 192.168.1.11:8080 / (none)
 *
 *
 * **As shown above, the language code or mapping is part of the authority:**
 *
 * <lh>Validity: authority / language code</lh>
 *  * Correct: "test.wikipedia.org" / "test"
 *  * Correct: "wikipedia.org", ""
 *  * Correct: "no.wikipedia.org", "nb"
 *  * Incorrect: "wikipedia.org", "test"
 *
 */
@Serializable
@Parcelize
class WikiSite(
    @SerialName("domain") @Serializable(with = UriSerializer::class) var uri: Uri,
    var languageCode: String = ""
) : Parcelable {

    constructor(uri: Uri) : this(uri, "") {
        val tempUri = ensureScheme(uri)
        var authority = tempUri.authority.orEmpty()
        if (("wikipedia.org" == authority || "www.wikipedia.org" == authority) &&
            tempUri.path?.startsWith("/wiki") == true
        ) {
            // Special case for Wikipedia only: assume English subdomain when none given.
            authority = "en.wikipedia.org"
        }

        // Unconditionally transform any mobile authority to canonical.
        authority = authority.replace(".m.", ".")
        languageCode = UriUtil.getLanguageVariantFromUri(tempUri).ifEmpty { authorityToLanguageCode(authority) }

        // This prevents showing mixed Chinese variants article when the URL is /zh/ or /wiki/ in zh.wikipedia.org
        if (languageCode == AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE) {
            languageCode = LanguageUtil.firstSelectedChineseVariant
        }

        // Use default subdomain in authority to prevent error when requesting endpoints. e.g. zh-tw.wikipedia.org
        if (authority.contains("wikipedia.org") && subdomain().isNotEmpty()) {
            authority = subdomain() + ".wikipedia.org"
        }
        this.uri = Uri.Builder().scheme(tempUri.scheme).encodedAuthority(authority).build()
    }

    constructor(url: String) : this(
        when {
            url.startsWith("http") -> Uri.parse(url)
            url.startsWith("//") -> Uri.parse("$DEFAULT_SCHEME:$url")
            else -> Uri.parse("$DEFAULT_SCHEME://$url")
        }
    )

    constructor(authority: String, languageCode: String) : this(authority) {
        this.languageCode = languageCode
    }

    fun scheme(): String {
        return uri.scheme.orEmpty().ifEmpty { DEFAULT_SCHEME }
    }

    fun authority(): String {
        return uri.authority.orEmpty()
    }

    fun subdomain(): String {
        return languageCodeToSubdomain(languageCode)
    }

    fun path(segment: String): String {
        return "/w/$segment"
    }

    fun url(): String {
        return uri.toString()
    }

    fun url(segment: String): String {
        return url() + path(segment)
    }

    // TODO: this method doesn't have much to do with WikiSite. Move to PageTitle?
    fun titleForInternalLink(internalLink: String?): PageTitle {
        // Strip the /wiki/ from the href
        return PageTitle(UriUtil.removeInternalLinkPrefix(internalLink.orEmpty()), this)
    }

    // TODO: this method doesn't have much to do with WikiSite. Move to PageTitle?
    fun titleForUri(uri: Uri): PageTitle {
        var path = uri.path
        if (!uri.fragment.isNullOrEmpty()) {
            path += "#" + uri.fragment
        }
        return titleForInternalLink(path)
    }

    fun dbName(): String {
        return subdomain().replace("-".toRegex(), "_") + "wiki"
    }

    // Necessary for building HashMaps based on WikiSites.
    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + languageCode.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WikiSite
        if (uri != other.uri) return false
        if (languageCode != other.languageCode) return false
        return true
    }

    companion object {
        const val DEFAULT_SCHEME = "https"
        private var DEFAULT_BASE_URL: String? = null

        @JvmStatic
        fun supportedAuthority(authority: String): Boolean {
            return authority.endsWith(Uri.parse(DEFAULT_BASE_URL).authority!!)
        }

        @JvmStatic
        fun setDefaultBaseUrl(url: String) {
            DEFAULT_BASE_URL = url.ifEmpty { Service.WIKIPEDIA_URL }
        }

        @JvmStatic
        fun forLanguageCode(languageCode: String): WikiSite {
            val uri = ensureScheme(Uri.parse(DEFAULT_BASE_URL))
            return WikiSite(
                (if (languageCode.isEmpty()) "" else languageCodeToSubdomain(languageCode) + ".") + uri.authority,
                languageCode
            )
        }

        fun normalizeLanguageCode(languageCode: String): String {
            return when (languageCode) {
                AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE -> AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE // T114042
                AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE -> AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE // T111853
                else -> languageCode
            }
        }

        private fun languageCodeToSubdomain(languageCode: String): String {
            return WikipediaApp.getInstance().language().getDefaultLanguageCode(languageCode) ?: normalizeLanguageCode(languageCode)
        }

        private fun authorityToLanguageCode(authority: String): String {
            val parts = authority.split("\\.".toRegex()).toTypedArray()
            val minLengthForSubdomain = 3
            return if (parts.size < minLengthForSubdomain ||
                parts.size == minLengthForSubdomain && parts[0] == "m"
            ) {
                // ""
                // wikipedia.org
                // m.wikipedia.org
                ""
            } else parts[0]
        }

        private fun ensureScheme(uri: Uri): Uri {
            return if (uri.scheme.isNullOrEmpty()) {
                uri.buildUpon().scheme(DEFAULT_SCHEME).build()
            } else uri
        }
    }
}
