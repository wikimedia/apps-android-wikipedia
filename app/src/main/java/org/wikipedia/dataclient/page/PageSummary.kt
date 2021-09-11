package org.wikipedia.dataclient.page

import android.location.Location
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.page.Page
import org.wikipedia.page.PageProperties
import org.wikipedia.page.PageTitle
import org.wikipedia.parcel.DateParceler
import org.wikipedia.util.UriUtil.getFilenameFromUploadUrl
import java.util.*

@JsonClass(generateAdapter = true)
@Parcelize
class PageSummary(
    val namespace: NamespaceContainer = NamespaceContainer(),
    var titles: Titles? = null,
    var lang: String = "",
    var thumbnail: Thumbnail? = null,
    var extract: String? = null,
    var description: String? = null,
    @Json(name = "originalimage") internal val originalImage: Thumbnail? = null,
    @Json(name = "wikibase_item") val wikiBaseItem: String? = null,
    @Json(name = "extract_html") val extractHtml: String? = null,
    @Json(name = "description_source") val descriptionSource: String = "",
    val geo: Location? = null,
    val type: String = TYPE_STANDARD,
    val pageId: Int = 0,
    val revision: Long = 0L,
    val timestamp: String = "",
    val views: Long = 0,
    internal val rank: Long = 0,
    @Json(name = "view_history") val viewHistory: List<ViewHistory> = emptyList()
) : Parcelable {
    val thumbnailUrl get() = thumbnail?.source
    val thumbnailWidth get() = thumbnail?.width ?: 0
    val thumbnailHeight get() = thumbnail?.height ?: 0
    val originalImageUrl get() = originalImage?.source
    val apiTitle get() = titles?.canonical.orEmpty()

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    val displayTitle get() = titles?.display.orEmpty()
    val leadImageName get() = thumbnailUrl?.let { getFilenameFromUploadUrl(it) }
    val ns: Namespace get() = Namespace.of(namespace.id)

    constructor(displayTitle: String, prefixTitle: String, description: String?,
                extract: String?, thumbnail: String?, lang: String) : this() {
        titles = Titles(prefixTitle, displayTitle)
        this.description = description
        this.extract = extract
        this.thumbnail = Thumbnail(thumbnail, 0, 0)
        this.lang = lang
    }

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

    @JsonClass(generateAdapter = true)
    @Parcelize
    data class NamespaceContainer(val id: Int = 0, val text: String = "") : Parcelable

    @JsonClass(generateAdapter = true)
    @Parcelize
    class Titles(val canonical: String?, val display: String?) : Parcelable

    @JsonClass(generateAdapter = true)
    @Parcelize
    class Thumbnail(val source: String?, val width: Int, val height: Int) : Parcelable

    @JsonClass(generateAdapter = true)
    @Parcelize
    @TypeParceler<Date, DateParceler>()
    class ViewHistory(val date: Date = Date(), val views: Float) : Parcelable

    companion object {
        const val TYPE_STANDARD = "standard"
        const val TYPE_DISAMBIGUATION = "disambiguation"
        const val TYPE_MAIN_PAGE = "mainpage"
        const val TYPE_NO_EXTRACT = "no-extract"
    }
}
