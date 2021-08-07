package org.wikipedia.gallery

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE
import org.wikipedia.dataclient.Service
import org.wikipedia.util.ImageUrlUtil
import java.util.*

@JsonClass(generateAdapter = true)
open class GalleryItem(
    @Json(name = "section_id")
    val sectionId: Int = 0,

    @Json(name = "wb_entity_id")
    val entityId: String = "",

    @Json(name = "audio_type")
    val audioType: String = "",

    @Json(name = "structured")
    var structuredData: StructuredData? = null,

    // return the base url of Wiki Commons for WikiSite() if the file_page is null.
    @Json(name = "file_page")
    var filePage: String = Service.COMMONS_URL,

    val duration: Double = 0.0,
    val isShowInGallery: Boolean = false,
    var type: String = "",
    var thumbnail: ImageInfo = ImageInfo(),
    var original: ImageInfo = ImageInfo(),
    var description: TextInfo = TextInfo(),
    val caption: TextInfo? = null,
    val sources: List<ImageInfo>? = null,
    var titles: Titles? = null,
    var artist: ArtistInfo? = null,
    var license: ImageLicense? = null,
) {
    val thumbnailUrl get() = thumbnail.source
    val preferredSizedImageUrl get() = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl, PREFERRED_GALLERY_IMAGE_SIZE)

    // The getSources has different levels of source,
    // should have an option that allows user to chose which quality to play
    val originalVideoSource get() = sources?.lastOrNull()

    var structuredCaptions
        get() = structuredData?.captions ?: emptyMap()
        set(captions) {
            if (structuredData == null) {
                structuredData = StructuredData()
            }
            structuredData?.captions = HashMap(captions)
        }

    @JsonClass(generateAdapter = true)
    class Titles(val display: String = "", val canonical: String = "", val normalized: String = "")

    @JsonClass(generateAdapter = true)
    class StructuredData(var captions: HashMap<String, String>? = null)
}
