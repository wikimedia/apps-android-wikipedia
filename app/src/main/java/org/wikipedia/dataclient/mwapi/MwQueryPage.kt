package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.util.DateUtil

@Serializable
class MwQueryPage {

    @SerialName("descriptionsource") val descriptionSource: String? = null
    @SerialName("imageinfo") private val imageInfo: List<ImageInfo>? = null
    @SerialName("videoinfo") private val videoInfo: List<ImageInfo>? = null
    @SerialName("watchlistexpiry") private val watchlistExpiry: String? = null
    @SerialName("pageviews") val pageViewsMap: Map<String, Long?> = emptyMap()
    @SerialName("pageid") val pageId = 0
    @SerialName("pageprops") val pageProps: PageProps? = null
    @SerialName("entityterms") val entityTerms: EntityTerms? = null

    private val ns = 0
    val coordinates: List<Coordinates>? = null
    private val thumbnail: Thumbnail? = null
    val varianttitles: Map<String, String>? = null
    private val actions: Map<String, List<MwServiceError>>? = null
    private val editintro: JsonElement? = null

    val index = 0
    var title: String = ""
    val langlinks: List<LangLink> = emptyList()
    val revisions: List<Revision> = emptyList()
    val protection: List<Protection> = emptyList()
    val extract: String? = null
    val description: String? = null
    private val imagerepository: String? = null
    var redirectFrom: String? = null
    var convertedFrom: String? = null
    var convertedTo: String? = null
    val watched = false
    val lastrevid: Long = 0

    val tasktype: String? = null
    val difficulty: String? = null
    val qualityGateIds: List<String>? = null
    val growthimagesuggestiondata: List<GrowthImageSuggestion>? = null

    fun namespace(): Namespace {
        return Namespace.of(ns)
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

    fun getEditNotices(): Map<String, String> {
        return if (editintro != null && editintro is JsonObject) JsonUtil.json.decodeFromJsonElement(editintro) else emptyMap()
    }

    @Serializable
    class Revision {
        private val slots: Map<String, RevisionSlot>? = null
        val minor = false
        @SerialName("revid") val revId: Long = 0
        @SerialName("parentid") val parentRevId: Long = 0
        @SerialName("anon") val isAnon = false
        @SerialName("timestamp") val timeStamp: String = ""
        val size = 0
        val user: String = ""
        val comment: String = ""
        val parsedcomment: String = ""

        val contentMain get() = getContentFromSlot("main")

        var diffSize = 0

        val localDateTime by lazy { DateUtil.iso8601LocalDateTimeParse(timeStamp) }

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
    class Coordinates(val lat: Double = 0.0, val lon: Double = 0.0)

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
    class EntityTerms {
        val alias: List<String> = emptyList()
        val label: List<String> = emptyList()
        val description: List<String> = emptyList()
    }
}
