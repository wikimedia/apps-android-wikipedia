package org.wikipedia.dataclient.page

import android.location.Location
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.*
import org.wikipedia.util.UriUtil.getFilenameFromUploadUrl

open class PageSummary {

    @SerializedName("originalimage")
    private val originalImage: Thumbnail? = null

    @SerializedName("wikibase_item")
    val wikiBaseItem: String? = null

    @SerializedName("extract_html")
    val extractHtml: String? = null

    @SerializedName("description_source")
    val descriptionSource = ""

    private var thumbnail: Thumbnail? = null
    private val namespace: NamespaceContainer? = null
    private var titles: Titles? = null
    var lang = ""
    var extract: String? = null
    var description: String? = null

    @JsonAdapter(GeoTypeAdapter::class)
    val geo: Location? = null
    val type = TYPE_STANDARD
    val pageId = 0
    val revision = 0L
    val timestamp = ""
    val thumbnailUrl get() = thumbnail?.url
    val thumbnailWidth get() = thumbnail?.width ?: 0
    val thumbnailHeight get() = thumbnail?.height ?: 0
    val originalImageUrl get() = originalImage?.url
    val apiTitle get() = titles?.canonical.orEmpty()

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    val displayTitle get() = titles?.display.orEmpty()

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

    fun getNamespace(): Namespace {
        return if (namespace == null) Namespace.MAIN else Namespace.of(namespace.id)
    }

    fun getPageTitle(wiki: WikiSite): PageTitle {
        return PageTitle(apiTitle, wiki, thumbnailUrl, description, displayTitle, extract)
    }

    private class Thumbnail(val url: String?, val width: Int, val height: Int)

    private class NamespaceContainer {
        val id = 0
    }

    private class Titles(val canonical: String?, val display: String?)

    override fun toString(): String {
        return displayTitle
    }

    val leadImageName get() = thumbnailUrl?.let { getFilenameFromUploadUrl(it) }

    companion object {
        const val TYPE_STANDARD = "standard"
        const val TYPE_DISAMBIGUATION = "disambiguation"
        const val TYPE_MAIN_PAGE = "mainpage"
        const val TYPE_NO_EXTRACT = "no-extract"
    }
}
