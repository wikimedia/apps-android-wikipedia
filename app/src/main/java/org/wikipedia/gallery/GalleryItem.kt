package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE
import org.wikipedia.dataclient.Service
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.StringUtil
import java.io.Serializable
import java.util.*

open class GalleryItem : Serializable {

    @SerializedName("section_id")
    val sectionId = 0

    @SerializedName("wb_entity_id")
    val entityId: String? = null

    @SerializedName("structured")
    var structuredData: StructuredData? = null

    @SerializedName("audio_type")
    val audioType: String = ""

    // return the base url of Wiki Commons for WikiSite() if the file_page is null.
    @SerializedName("file_page")
    var filePage: String = Service.COMMONS_URL

    var type: String = ""
    val isShowInGallery = false
    var thumbnail = ImageInfo()
    var original = ImageInfo()
    var description = TextInfo()
    val duration = 0.0
    var titles: Titles? = null
    val caption: TextInfo? = null
    val sources: List<ImageInfo>? = null
    var artist: ArtistInfo? = null
    var license: ImageLicense? = null
    val thumbnailUrl
        get() = thumbnail.source
    val preferredSizedImageUrl
        get() = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl, PREFERRED_GALLERY_IMAGE_SIZE)

    protected fun setTitle(title: String) {
        titles = Titles(title, StringUtil.addUnderscores(title), title)
    }

    // The getSources has different levels of source,
    // should have an option that allows user to chose which quality to play
    val originalVideoSource
        get() =// The getSources has different levels of source,
            // should have an option that allows user to chose which quality to play
            if (sources.isNullOrEmpty()) null else sources[sources.size - 1]

    var structuredCaptions
        get() = if (structuredData != null && structuredData!!.captions != null) structuredData!!.captions!! else emptyMap()
        set(captions) {
            if (structuredData == null) {
                structuredData = StructuredData()
            }
            structuredData!!.captions = HashMap(captions)
        }

    class Titles internal constructor(display: String, canonical: String, normalized: String) : Serializable {

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
