package org.wikipedia.gallery

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.Service
import org.wikipedia.util.UriUtil

@Serializable
@Parcelize
class MediaListItem constructor(val title: String = "",
                                val type: String = "",
                                val caption: TextInfo? = null,
                                val showInGallery: Boolean = false,
                                @SerialName("section_id") private val sectionId: Int = 0,
                                @SerialName("srcset") val srcSets: List<ImageSrcSet> = emptyList()) :
    Parcelable {

    val isInCommons get() = srcSets.firstOrNull()?.src?.contains(Service.URL_FRAGMENT_FROM_COMMONS) == true

    fun getImageUrl(deviceScale: Float): String {
        var imageUrl = srcSets[0].src
        var lastScale = 1.0f
        srcSets.forEach { srcSet ->
            val scale = srcSet.scale
            if (deviceScale >= scale && lastScale < scale) {
                lastScale = scale
                imageUrl = srcSet.src
            }
        }
        return UriUtil.resolveProtocolRelativeUrl(imageUrl)
    }

    @Serializable
    @Parcelize
    class ImageSrcSet(
            @SerialName("scale") private val _scale: String? = null,
            val src: String = ""
    ) : Parcelable {
        val scale get() = _scale?.replace("x", "")?.toFloat() ?: 0f
    }
}
