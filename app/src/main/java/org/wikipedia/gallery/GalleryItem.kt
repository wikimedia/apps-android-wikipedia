package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE
import org.wikipedia.dataclient.Service
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import org.wikipedia.util.StringUtil.addUnderscores
import java.io.Serializable
import java.util.*

open class GalleryItem : Serializable {

    @SerializedName("section_id")
    private val sectionId = 0
    private var type: String? = null

    @SerializedName("audio_type")
    val audioType: String? = null
        get() = StringUtils.defaultString(field)
    val caption: TextInfo? = null
    val isShowInGallery = false
    var titles: Titles? = null
        private set
    private var thumbnail: ImageInfo? = null
    private var original: ImageInfo? = null
    val sources: List<ImageInfo>? = null

    // return the base url of Wiki Commons for WikiSite() if the file_page is null.
    @SerializedName("file_page")
    var filePage: String? = null
        get() =// return the base url of Wiki Commons for WikiSite() if the file_page is null.
            StringUtils.defaultString(field, Service.COMMONS_URL)
    var artist: ArtistInfo? = null
    val duration = 0.0
    var license: ImageLicense? = null
    private var description: TextInfo? = null

    @SerializedName("wb_entity_id")
    private val entityId: String? = null

    @SerializedName("structured")
    private var structuredData: StructuredData? = null

    constructor() {}
    constructor(title: String) {
        type = "*/*"
        titles = Titles(title, addUnderscores(title), title)
        original = ImageInfo()
        thumbnail = ImageInfo()
        description = TextInfo()
        license = ImageLicense()
    }

    fun getType(): String {
        return StringUtils.defaultString(type)
    }

    protected fun setTitle(title: String) {
        titles = Titles(title, addUnderscores(title), title)
    }

    fun getThumbnail(): ImageInfo {
        if (thumbnail == null) {
            thumbnail = ImageInfo()
        }
        return thumbnail!!
    }

    val thumbnailUrl: String
        get() = getThumbnail().source
    val preferredSizedImageUrl: String
        get() = getUrlForPreferredSize(thumbnailUrl, PREFERRED_GALLERY_IMAGE_SIZE)

    fun getOriginal(): ImageInfo {
        if (original == null) {
            original = ImageInfo()
        }
        return original!!
    }

    // The getSources has different levels of source,
    // should have an option that allows user to chose which quality to play
    val originalVideoSource: ImageInfo?
        get() =// The getSources has different levels of source,
            // should have an option that allows user to chose which quality to play
            if (sources.isNullOrEmpty()) null else sources[sources.size - 1]

    fun getDescription(): TextInfo {
        if (description == null) {
            description = TextInfo()
        }
        return description!!
    }

    var structuredCaptions: Map<String, String>
        get() = if (structuredData != null && structuredData!!.captions != null) structuredData!!.captions!! else emptyMap()
        set(captions) {
            if (structuredData == null) {
                structuredData = StructuredData()
            }
            structuredData!!.captions = HashMap(captions)
        }

    class Titles internal constructor(display: String, canonical: String, normalized: String) :
        Serializable {

        var canonical: String = ""
        var normalized: String = ""
        var display: String = ""

        init {
            this.display = display
            this.canonical = canonical
            this.normalized = normalized
        }
    }

    class StructuredData : Serializable {

        var captions: HashMap<String, String>? = null
    }
}
