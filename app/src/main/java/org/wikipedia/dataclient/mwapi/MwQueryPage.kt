package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.Namespace

@Serializable
class MwQueryPage {

    @SerialName("descriptionsource") val descriptionSource: String? = null
    @SerialName("imageinfo") private val imageInfo: List<ImageInfo>? = null
    @SerialName("videoinfo") private val videoInfo: List<ImageInfo>? = null
    @SerialName("watchlistexpiry") private val watchlistExpiry: String? = null
    @SerialName("pageviews") val pageViewsMap: Map<String, Long?> = emptyMap()
    @SerialName("imagelabels") val imageLabels: List<ImageLabel> = emptyList()
    @SerialName("pageid") val pageId = 0
    @SerialName("pageprops") val pageProps: PageProps? = null

    private val ns = 0
    private var coordinates: List<Coordinates>? = null
    private val thumbnail: Thumbnail? = null
    private val varianttitles: Map<String, String>? = null
    private val actions: Map<String, List<MwServiceError>>? = null

    val index = 0
    var title: String = ""
    val langlinks: List<LangLink> = emptyList()
    val revisions: List<Revision> = emptyList()
    val categories: List<Category>? = null
    val protection: List<Protection> = emptyList()
    val extract: String? = null
    val description: String? = null
    private val imagerepository: String? = null
    var redirectFrom: String? = null
    var convertedFrom: String? = null
    var convertedTo: String? = null
    val isWatched = false
    val lastRevId: Long = 0

    fun namespace(): Namespace {
        return Namespace.of(ns)
    }

    fun coordinates(): List<Coordinates>? {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        coordinates = coordinates?.filterNotNull()
        return coordinates
    }

    fun thumbUrl(): String? {
        return thumbnail?.source
    }

    fun imageInfo(): ImageInfo? {
        return imageInfo?.get(0) ?: videoInfo?.get(0)
    }

    fun appendTitleFragment(fragment: String?) {
        title += "#$fragment"
    }

    fun displayTitle(langCode: String): String {
        return varianttitles?.get(langCode).orEmpty().ifEmpty { title }
    }

    val isImageShared: Boolean
        get() = imagerepository.orEmpty() == "shared"

    fun hasWatchlistExpiry(): Boolean {
        return !watchlistExpiry.isNullOrEmpty()
    }

    fun getErrorForAction(actionName: String): List<MwServiceError> {
        return actions?.get(actionName) ?: emptyList()
    }

    @Serializable
    class Revision {

        @SerialName("contentformat") private val contentFormat: String? = null
        @SerialName("contentmodel") private val contentModel: String? = null
        @SerialName("timestamp") val timeStamp: String = ""

        private val slots: Map<String, RevisionSlot>? = null
        private val minor = false
        @SerializedName("revid") val revId: Long = 0
        @SerializedName("parentid") val parentRevId: Long = 0
        @SerializedName("anon") val isAnon = false
        val user: String = ""
        val content: String = ""
        val comment: String = ""

        fun getContentFromSlot(slot: String): String {
            return slots?.get(slot)?.content.orEmpty()
        }
    }

    @Serializable
    class RevisionSlot(val content: String = "",
                       private val contentformat: String? = null,
                       private val contentmodel: String? = null)

    @Serializable
    class LangLink(val lang: String = "", val title: String = "")

    @Serializable
    class Coordinates(val lat: Double? = null, val lon: Double? = null)

    @Serializable
    internal class Thumbnail(val source: String? = null,
                             private val width: Int = 0,
                             private val height: Int = 0)

    @Serializable
    class PageProps {

        @SerialName("wikibase_item") val wikiBaseItem: String = ""
        private val disambiguation: String? = null
        @SerialName("displaytitle") val displayTitle: String? = null
    }

    @Serializable
    class Category(val ns: Int = 0, val title: String = "", val hidden: Boolean = false)

    @Serializable
    class ImageLabel {

        @SerialName("wikidata_id") var wikidataId: String? = ""
        private val confidence: Confidence? = null
        val state: String = ""
        var label: String = ""
        var description: String? = ""
        var isSelected = false
        var isCustom = false

        constructor()
        constructor(wikidataId: String, label: String, description: String?) {
            this.wikidataId = wikidataId
            this.label = label
            this.description = description
            isCustom = true
        }

        val confidenceScore: Float
            get() = confidence?.google ?: 0f
    }

    @Serializable
    class Confidence(val google: Float = 0f)
}
