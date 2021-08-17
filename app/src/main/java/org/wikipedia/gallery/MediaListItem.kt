package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.Service
import org.wikipedia.util.UriUtil
import java.io.Serializable

class MediaListItem constructor(val title: String = "",
                                val type: String = "",
                                val caption: TextInfo? = null,
                                val showInGallery: Boolean = false,
                                @SerializedName("section_id") private val sectionId: Int = 0,
                                @SerializedName("srcset") val srcSets: List<ImageSrcSet> = emptyList()) : Serializable {

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

    inner class ImageSrcSet : Serializable {

        @SerializedName("scale")
        private val _scale: String? = null
        val src: String = ""
        val scale get() = _scale?.replace("x", "")?.toFloat() ?: 0f
    }
}
