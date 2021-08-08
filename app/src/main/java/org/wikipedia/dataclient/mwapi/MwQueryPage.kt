package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.Namespace
import org.wikipedia.page.Namespace.Companion.of

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
@JsonClass(generateAdapter = true)
class MwQueryPage(
    @Json(name = "pageid") val pageId: Int = 0,
    val ns: Int = 0,
    val index: Int = 0,
    @Json(name = "lastrevid") val lastRevId: Long = 0,
    var title: String = "",
    @Json(name = "langlinks") val langLinks: List<LangLink> = emptyList(),
    val revisions: List<Revision> = emptyList(),
    val coordinates: List<Coordinates> = emptyList(),
    val categories: List<Category> = emptyList(),
    val protection: List<Protection> = emptyList(),
    val pageProps: PageProps? = null,
    val extract: String? = null,
    val thumbnail: Thumbnail? = null,
    val description: String? = null,
    @Json(name = "descriptionsource") val descriptionSource: String? = null,
    @Json(name = "imageinfo") val imageInfo: List<ImageInfo> = emptyList(),
    @Json(name = "videoinfo") val videoInfo: List<ImageInfo> = emptyList(),
    @Json(name = "imagerepository") val imageRepository: String? = null,
    var redirectFrom: String? = null,
    var convertedFrom: String? = null,
    var convertedTo: String? = null,
    @Json(name = "varianttitles") val variantTitles: Map<String, String> = emptyMap(),
    @Json(name = "pageviews") val pageViewsMap: Map<String, Long> = emptyMap(),
    @Json(name = "imagelabels") val imageLabels: List<ImageLabel> = emptyList(),
    @Json(name = "watchlistexpiry") val watchlistExpiry: String? = null,
    val actions: Map<String, List<MwServiceError>> = emptyMap(),
    @Json(name = "watched") val isWatched: Boolean = false
) {
    val namespace: Namespace
        get() = of(ns)
    val thumbUrl: String?
        get() = thumbnail?.source
    val firstImageInfo: ImageInfo
        get() = imageInfo[0]
    val isImageShared: Boolean
        get() = "shared" == imageRepository

    fun appendTitleFragment(fragment: String?) {
        title += "#$fragment"
    }

    fun getDisplayTitle(langCode: String): String {
        return variantTitles[langCode].orEmpty().ifEmpty { title }
    }

    fun hasWatchlistExpiry(): Boolean {
        return !watchlistExpiry.isNullOrEmpty()
    }

    fun getErrorForAction(actionName: String): List<MwServiceError> {
        return actions[actionName] ?: emptyList()
    }

    @JsonClass(generateAdapter = true)
    class Revision(
        @Json(name = "revid") val revId: Long = 0,
        @Json(name = "parentid") val parentRevId: Long = 0,
        val minor: Boolean = false,
        @Json(name = "anon") val isAnon: Boolean = false,
        val user: String = "",
        @Json(name = "contentformat") val contentFormat: String = "",
        @Json(name = "contentmodel") val contentModel: String = "",
        @Json(name = "timestamp") val timeStamp: String = "",
        val content: String = "",
        val comment: String = "",
        val slots: Map<String, RevisionSlot> = emptyMap()
    ) {
        fun getContentFromSlot(slot: String): String {
            return slots[slot]?.content.orEmpty()
        }
    }

    @JsonClass(generateAdapter = true)
    class RevisionSlot(internal val contentmodel: String?, internal val contentformat: String?, val content: String)

    @JsonClass(generateAdapter = true)
    class LangLink(val lang: String, val title: String)

    @JsonClass(generateAdapter = true)
    class Coordinates(val lan: Double?, val lon: Double?)

    @JsonClass(generateAdapter = true)
    class Thumbnail(val source: String, val width: Int, val height: Int)

    @JsonClass(generateAdapter = true)
    class PageProps(@Json(name = "wikibase_item") val wikiBaseItem: String,
                    @Json(name = "displaytitle") val displayTitle: String?,
                    val disambiguation: String?) {
        val isDisambiguation: Boolean
            get() = disambiguation != null
    }

    @JsonClass(generateAdapter = true)
    class Category(val ns: Int, val title: String, val hidden: Boolean)

    @JsonClass(generateAdapter = true)
    class ImageLabel(
        @Json(name = "wikidata_id") val wikidataId: String = "",
        val confidence: Confidence? = null,
        val state: String = "",
        val label: String = "",
        val description: String = "",
        @Json(name = "selected") var isSelected: Boolean = false,
        val isCustom: Boolean = false
    ) {
        val confidenceScore: Float
            get() = confidence?.google ?: 0f
    }

    @JsonClass(generateAdapter = true)
    class Confidence(val google: Float)
}
