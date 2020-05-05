package org.wikipedia.dataclient.page

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.Service
import org.wikipedia.page.Namespace
import org.wikipedia.page.Section
import org.wikipedia.util.UriUtil

@Suppress("UNUSED")
class PageLead {
    private val ns = 0
    val id = 0
    val revision: Long = 0

    @SerializedName("lastmodified")
    val lastModified: String? = null

    @SerializedName("displaytitle")
    val displayTitle: String? = null
    val redirected: String? = null

    @SerializedName("normalizedtitle")
    val normalizedTitle: String? = null

    @SerializedName("wikibase_item")
    val wikiBaseItem: String? = null

    @SerializedName("pronunciation")
    private val titlePronunciation: TitlePronunciation? = null

    @SerializedName("languagecount")
    private val languageCount = 0
    private val editable = false

    @SerializedName("mainpage")
    val isMainPage = false
    val isDisambiguation = false
    val description: String? = null

    @SerializedName("description_source")
    val descriptionSource: String? = null
    private val image: Image? = null
    private val sections: List<Section>? = null
    val leadSectionContent: String
        get() = sections?.get(0)?.content ?: ""

    val namespace: Namespace
        get() = Namespace.of(ns)

    val titlePronunciationUrl: String?
        get() = if (titlePronunciation == null) null else UriUtil.resolveProtocolRelativeUrl(titlePronunciation.url!!)

    fun getLeadImageUrl(leadImageWidth: Int) = image?.getUrl(leadImageWidth)

    val thumbUrl: String?
        get() = image?.getUrl(Service.PREFERRED_THUMB_SIZE)

    val leadImageFileName: String?
        get() = image?.fileName

    fun getSections() = sections ?: emptyList()

    /**
     * For the lead image File: page name
     */
    class TitlePronunciation {
        val url: String? = null
    }

    /**
     * For the lead image File: page name
     */
    class Image {
        @SerializedName("file")
        val fileName: String? = null
        private val urls: ThumbUrls? = null

        fun getUrl(width: Int) = urls?.get(width)
    }

    /**
     * For the lead image URLs
     */
    class ThumbUrls {
        @SerializedName("320")
        private val small: String? = null

        @SerializedName("640")
        private val medium: String? = null

        @SerializedName("800")
        private val large: String? = null

        @SerializedName("1024")
        private val xl: String? = null
        operator fun get(width: Int): String? {
            return when (width) {
                SMALL -> small
                MEDIUM -> medium
                LARGE -> large
                XL -> xl
                else -> null
            }
        }

        companion object {
            private const val SMALL = 320
            private const val MEDIUM = 640
            private const val LARGE = 800
            private const val XL = 1024
        }
    }
}
