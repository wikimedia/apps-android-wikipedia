package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE
import org.wikipedia.dataclient.Service
import org.wikipedia.util.ImageUrlUtil
import java.util.*

@Serializable
open class GalleryItem {

    @SerializedName("section_id")
    val sectionId = 0

    @SerializedName("wb_entity_id")
    val entityId: String = ""

    @SerializedName("audio_type")
    val audioType: String = ""

    @SerializedName("structured")
    var structuredData: StructuredData? = null

    // return the base url of Wiki Commons for WikiSite() if the file_page is null.
    @SerializedName("file_page")
    var filePage: String = Service.COMMONS_URL

    val duration = 0.0
    val isShowInGallery = false
    var type: String = ""
    var thumbnail = ImageInfo()
    var original = ImageInfo()
    var description = TextInfo()

    val caption: TextInfo? = null
    val sources: List<ImageInfo>? = null
    var titles: Titles? = null
    var artist: ArtistInfo? = null
    var license: ImageLicense? = null

    val thumbnailUrl get() = thumbnail.source
    val preferredSizedImageUrl get() = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl, PREFERRED_GALLERY_IMAGE_SIZE)

    // The getSources has different levels of source,
    // should have an option that allows user to chose which quality to play
    val originalVideoSource get() = sources?.getOrNull(sources.size - 1)

    var structuredCaptions
        get() = structuredData?.captions ?: emptyMap()
        set(captions) {
            if (structuredData == null) {
                structuredData = StructuredData()
            }
            structuredData?.captions = HashMap(captions)
        }

    @Serializable
    class Titles constructor(val display: String = "",
                                                                    val canonical: String = "",
                                                                    val normalized: String = "")

    @Serializable
    class StructuredData(var captions: HashMap<String, String>? = null)
}
