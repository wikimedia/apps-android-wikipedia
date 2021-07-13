package org.wikipedia.dataclient.page

import android.location.Location
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.*
import org.wikipedia.page.Namespace.Companion.of
import org.wikipedia.util.UriUtil.getFilenameFromUploadUrl

open class PageSummary {

    @SerializedName("extract_html")
    val extractHtml: String? = null

    @SerializedName("description_source")
    val descriptionSource = ""

    @SerializedName("originalimage")
    private val originalImage: Thumbnail? = null

    @SerializedName("wikibase_item")
    private var titles: Titles? = null

    private val namespace: NamespaceContainer? = null
    private var thumbnail: Thumbnail? = null
    var lang: String = ""
    var extract: String? = null

    @JsonAdapter(GeoTypeAdapter::class)
    val geo: Location? = null
    var description: String? = null
    val wikiBaseItem: String = ""
    val type = TYPE_STANDARD
    val pageId = 0
    val revision: Long = 0
    val timestamp: String = ""


    constructor()
    constructor(displayTitle: String, prefixTitle: String, description: String?,
                extract: String?, thumbnail: String?, lang: String) {
        titles = Titles(prefixTitle, displayTitle)
        this.description = description
        this.extract = extract
        this.thumbnail = Thumbnail(thumbnail, 0, 0)
        this.lang = lang
    }

    fun toPage(title: PageTitle): Page {
        return Page(adjustPageTitle(title), PageProperties(this))
    }

    private fun adjustPageTitle(title: PageTitle): PageTitle {
        var newTitle = title
        if (titles != null && titles!!.canonical != null) {
            newTitle = PageTitle(titles!!.canonical, title.wikiSite, title.thumbUrl)
            newTitle.fragment = title.fragment
        }
        newTitle.description = description
        return newTitle
    }

    val apiTitle: String
        get() = StringUtils.defaultString(if (titles != null) titles!!.canonical else null)

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    val displayTitle: String
        get() = StringUtils.defaultString(if (titles != null) titles!!.display else null)

    fun getNamespace(): Namespace {
        return if (namespace == null) Namespace.MAIN else of(namespace.id())
    }

    val thumbnailUrl: String?
        get() = if (thumbnail == null) null else thumbnail!!.url
    val thumbnailWidth: Int
        get() = if (thumbnail == null) 0 else thumbnail!!.width
    val thumbnailHeight: Int
        get() = if (thumbnail == null) 0 else thumbnail!!.height
    val originalImageUrl: String?
        get() = originalImage?.url

    fun getPageTitle(wiki: WikiSite): PageTitle {
        return PageTitle(apiTitle, wiki, thumbnailUrl, description, displayTitle, extract)
    }

    private class Thumbnail(val url: String?, val width: Int, val height: Int)
    private class NamespaceContainer {
        private val id = 0
        private val text: String? = null
        fun id(): Int {
            return id
        }
    }

    private class Titles(val canonical: String?, val display: String?)

    override fun toString(): String {
        return displayTitle
    }

    val leadImageName: String?
        get() = if (thumbnailUrl == null) null else getFilenameFromUploadUrl(thumbnailUrl!!)

    companion object {
        const val TYPE_STANDARD = "standard"
        const val TYPE_DISAMBIGUATION = "disambiguation"
        const val TYPE_MAIN_PAGE = "mainpage"
        const val TYPE_NO_EXTRACT = "no-extract"
    }
}
