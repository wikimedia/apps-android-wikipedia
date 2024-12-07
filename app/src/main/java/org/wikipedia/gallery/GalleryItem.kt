package org.wikipedia.gallery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.Service

@Suppress("unused")
@Serializable
open class GalleryItem {

    @SerialName("section_id")
    val sectionId = 0

    @SerialName("wb_entity_id")
    val entityId: String = ""

    @SerialName("audio_type")
    val audioType: String = ""

    @SerialName("structured")
    var structuredData: StructuredData? = null

    @SerialName("file_page")
    var filePage: String = Service.COMMONS_URL

    val duration = 0.0
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

    @Serializable
    class Titles(val display: String = "", val canonical: String = "", val normalized: String = "")

    @Serializable
    class StructuredData(var captions: HashMap<String, String>? = null)
}
