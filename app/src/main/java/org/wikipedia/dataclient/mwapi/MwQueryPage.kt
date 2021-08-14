package org.wikipedia.dataclient.mwapi

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.Namespace
import org.wikipedia.page.Namespace.Companion.of

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
class MwQueryPage {
    private val pageid = 0
    private val ns = 0
    private val index = 0
    val lastRevId: Long = 0
    private var title: String? = null
    private val langlinks: List<LangLink>? = null
    private val revisions: List<Revision>? = null
    private var coordinates: List<Coordinates>? = null
    private val categories: List<Category>? = null
    private val protection: List<Protection>? = null
    private val pageprops: PageProps? = null
    private val extract: String? = null
    private val thumbnail: Thumbnail? = null
    private val description: String? = null

    @SerializedName("descriptionsource")
    private val descriptionSource: String? = null

    @SerializedName("imageinfo")
    private val imageInfo: List<ImageInfo>? = null

    @SerializedName("videoinfo")
    private val videoInfo: List<ImageInfo>? = null
    private val imagerepository: String? = null
    private var redirectFrom: String? = null
    private var convertedFrom: String? = null
    private var convertedTo: String? = null
    private val varianttitles: Map<String, String>? = null

    @SerializedName("pageviews")
    val pageViewsMap: Map<String, Long>? = null
        get() = field ?: emptyMap()

    @SerializedName("imagelabels")
    val imageLabels: List<ImageLabel>? = null
        get() = field ?: emptyList()

    @SerializedName("watchlistexpiry")
    private val watchlistExpiry: String? = null
    private val actions: Map<String, List<MwServiceError>>? = null
    val isWatched = false
    fun title(): String {
        return StringUtils.defaultString(title)
    }

    fun index(): Int {
        return index
    }

    fun namespace(): Namespace {
        return of(ns)
    }

    fun langLinks(): List<LangLink>? {
        return langlinks
    }

    fun revisions(): List<Revision> {
        return revisions ?: emptyList()
    }

    fun categories(): List<Category>? {
        return categories
    }

    fun protection(): List<Protection> {
        return protection ?: emptyList()
    }

    fun coordinates(): List<Coordinates>? {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        coordinates = coordinates?.filterNotNull()
        return coordinates
    }

    fun pageId(): Int {
        return pageid
    }

    fun pageProps(): PageProps? {
        return pageprops
    }

    fun extract(): String? {
        return extract
    }

    fun thumbUrl(): String? {
        return thumbnail?.source
    }

    fun description(): String? {
        return description
    }

    fun descriptionSource(): String? {
        return descriptionSource
    }

    fun imageInfo(): ImageInfo? {
        return imageInfo?.get(0) ?: videoInfo?.get(0)
    }

    fun redirectFrom(): String? {
        return redirectFrom
    }

    fun redirectFrom(from: String?) {
        redirectFrom = from
    }

    fun convertedFrom(): String? {
        return convertedFrom
    }

    fun convertedFrom(from: String?) {
        convertedFrom = from
    }

    fun convertedTo(): String? {
        return convertedTo
    }

    fun convertedTo(to: String?) {
        convertedTo = to
    }

    fun appendTitleFragment(fragment: String?) {
        title += "#$fragment"
    }

    fun displayTitle(langCode: String): String {
        return if (varianttitles != null) StringUtils.defaultIfEmpty(varianttitles[langCode], title())!! else title()
    }

    val isImageShared: Boolean
        get() = StringUtils.defaultString(imagerepository) == "shared"

    fun hasWatchlistExpiry(): Boolean {
        return !TextUtils.isEmpty(watchlistExpiry)
    }

    fun getErrorForAction(actionName: String): List<MwServiceError> {
        return if (actions != null && actions.containsKey(actionName)) actions[actionName]!! else emptyList()
    }

    class Revision {
        val revId: Long = 0
        val parentRevId: Long = 0
        private val minor = false
        val isAnon = false
        val user: String? = null
            get() = StringUtils.defaultString(field)

        @SerializedName("contentformat")
        private val contentFormat: String? = null

        @SerializedName("contentmodel")
        private val contentModel: String? = null

        @SerializedName("timestamp")
        private val timeStamp: String? = null
        private val content: String? = null
        val comment: String? = null
            get() = StringUtils.defaultString(field)
        private val slots: Map<String, RevisionSlot>? = null
        fun content(): String {
            return StringUtils.defaultString(content)
        }

        fun timeStamp(): String {
            return StringUtils.defaultString(timeStamp)
        }

        fun getContentFromSlot(slot: String): String {
            return if (slots != null && slots.containsKey(slot)) slots[slot]!!.content!! else ""
        }
    }

    class RevisionSlot {
        private val contentmodel: String? = null
        private val contentformat: String? = null
        val content: String? = null
            get() = StringUtils.defaultString(field)
    }

    class LangLink {

        val lang: String = ""
        val title: String = ""
    }

    class Coordinates {

        val lat: Double? = null
        val lon: Double? = null
    }

    internal class Thumbnail {

        val source: String? = null
        private val width = 0
        private val height = 0
    }

    class PageProps {

        @SerializedName("wikibase_item")
        val wikiBaseItem: String = ""
        val displayTitle: String? = null
        private val disambiguation: String? = null

        fun isDisambiguation(): Boolean {
            return disambiguation.isNullOrEmpty()
        }
    }

    class Category {

        val ns = 0
        val title: String = ""
        val hidden = false
    }

    class ImageLabel {

        @SerializedName("wikidata_id")
        var wikidataId: String? = ""
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

    class Confidence {

        val google = 0f
    }
}
