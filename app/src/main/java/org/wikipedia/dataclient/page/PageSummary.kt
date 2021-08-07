package org.wikipedia.dataclient.page

import android.location.Location
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.page.Page
import org.wikipedia.page.PageProperties
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil

@JsonClass(generateAdapter = true)
open class PageSummary(
    internal var thumbnail: Thumbnail? = null,
    internal var titles: Titles? = null,

    @Json(name = "namespace")
    internal val _namespace: NamespaceContainer? = null,

    @Json(name = "originalimage")
    internal val originalImage: Thumbnail? = null,

    @Json(name = "wikibase_item")
    val wikiBaseItem: String? = null,

    @Json(name = "extract_html")
    val extractHtml: String? = null,

    @Json(name = "description_source")
    val descriptionSource: String = "",

    var lang: String = "",
    var extract: String? = null,
    var description: String? = null,
    val geo: Location? = null,
    val type: String = TYPE_STANDARD,
    val pageId: Int = 0,
    val revision: Long = 0L,
    val timestamp: String = ""
) {
    val thumbnailUrl get() = thumbnail?.url
    val thumbnailWidth get() = thumbnail?.width ?: 0
    val thumbnailHeight get() = thumbnail?.height ?: 0
    val originalImageUrl get() = originalImage?.url
    val namespace get() = _namespace?.let { Namespace.of(_namespace.id) } ?: Namespace.MAIN
    val leadImageName get() = thumbnailUrl?.let { UriUtil.getFilenameFromUploadUrl(it) }
    val apiTitle get() = titles?.canonical.orEmpty()

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    val displayTitle get() = titles?.display.orEmpty()

    constructor(
        displayTitle: String, prefixTitle: String, description: String?,
        extract: String?, thumbnail: String?, lang: String
    ) : this(
        titles = Titles(prefixTitle, displayTitle), description = description, extract = extract,
        thumbnail = Thumbnail(thumbnail, 0, 0), lang = lang
    )

    fun toPage(title: PageTitle?): Page? {
        return title?.let { Page(adjustPageTitle(it), pageProperties = PageProperties(this)) }
    }

    fun getPageTitle(wiki: WikiSite): PageTitle {
        return PageTitle(apiTitle, wiki, thumbnailUrl, description, displayTitle, extract)
    }

    private fun adjustPageTitle(title: PageTitle): PageTitle {
        var newTitle = title
        titles?.canonical?.let {
            newTitle = PageTitle(it, title.wikiSite, title.thumbUrl)
            newTitle.fragment = title.fragment
        }
        newTitle.description = description
        return newTitle
    }

    override fun toString(): String {
        return displayTitle
    }

    @JsonClass(generateAdapter = true)
    class Thumbnail(@Json(name = "source") val url: String? = null, val width: Int = 0, val height: Int = 0)

    @JsonClass(generateAdapter = true)
    class NamespaceContainer(val id: Int = 0)

    @JsonClass(generateAdapter = true)
    class Titles(val canonical: String? = null, val display: String? = null)

    companion object {
        const val TYPE_STANDARD = "standard"
        const val TYPE_DISAMBIGUATION = "disambiguation"
        const val TYPE_MAIN_PAGE = "mainpage"
        const val TYPE_NO_EXTRACT = "no-extract"
    }
}
