package org.wikipedia.dataclient.page

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.LocationSerializer
import org.wikipedia.page.Namespace
import org.wikipedia.page.Page
import org.wikipedia.page.PageProperties
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil.getFilenameFromUploadUrl

@Parcelize
@Serializable
open class PageSummary(
    val namespace: NamespaceContainer? = null,
    var titles: Titles? = null,
    var lang: String = "",
    var thumbnail: Thumbnail? = null,
    var extract: String? = null,
    var description: String? = null,
    @SerialName("originalimage") private val originalImage: Thumbnail? = null,
    @SerialName("wikibase_item") val wikiBaseItem: String? = null,
    @SerialName("extract_html") val extractHtml: String? = null,
    @SerialName("description_source") val descriptionSource: String = "",
    @Serializable(with = LocationSerializer::class) val geo: Location? = null,
    val type: String = TYPE_STANDARD,
    @SerialName("pageid") val pageId: Int = 0,
    val revision: Long = 0L,
    val timestamp: String = "",
    val views: Long = 0,
    private val rank: Long = 0,
    @SerialName("view_history") val viewHistory: List<ViewHistory>? = null
) : Parcelable {

    val thumbnailUrl get() = thumbnail?.source
    val thumbnailWidth get() = thumbnail?.width ?: 0
    val thumbnailHeight get() = thumbnail?.height ?: 0
    val originalImageUrl get() = originalImage?.source
    val apiTitle get() = titles?.canonical.orEmpty()

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    val displayTitle get() = titles?.display.orEmpty()

    val leadImageName get() = thumbnailUrl?.let { getFilenameFromUploadUrl(it) }

    val ns: Namespace get() = if (namespace == null) Namespace.MAIN else Namespace.of(namespace.id)

    constructor(displayTitle: String, prefixTitle: String, description: String?,
                extract: String?, thumbnail: String?, lang: String) : this(
        titles = Titles(prefixTitle, displayTitle), description = description, extract = extract,
        thumbnail = Thumbnail(thumbnail, 0, 0), lang = lang
    )

    fun toPage(title: PageTitle): Page {
        return Page(adjustPageTitle(title), pageProperties = PageProperties(this))
    }

    private fun adjustPageTitle(title: PageTitle): PageTitle {
        var newTitle = title
        if (titles?.canonical != null) {
            newTitle = PageTitle(titles?.canonical, title.wikiSite, title.thumbUrl)
            newTitle.fragment = title.fragment
        }
        newTitle.description = description
        return newTitle
    }

    fun getPageTitle(wiki: WikiSite): PageTitle {
        return PageTitle(apiTitle, wiki, thumbnailUrl, description, displayTitle, extract)
    }

    override fun toString(): String {
        return displayTitle
    }

    @Parcelize
    @Serializable
    data class NamespaceContainer(val id: Int = 0, val text: String = "") : Parcelable

    @Parcelize
    @Serializable
    class Titles(val canonical: String?, val display: String?) : Parcelable

    @Parcelize
    @Serializable
    class Thumbnail(val source: String?, val width: Int, val height: Int) : Parcelable

    @Parcelize
    @Serializable
    class ViewHistory(val date: String, val views: Float) : Parcelable

    companion object {
        const val TYPE_STANDARD = "standard"
        const val TYPE_DISAMBIGUATION = "disambiguation"
        const val TYPE_MAIN_PAGE = "mainpage"
        const val TYPE_NO_EXTRACT = "no-extract"
    }
}
