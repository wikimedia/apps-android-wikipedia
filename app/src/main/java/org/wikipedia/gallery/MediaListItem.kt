package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.Service
import org.wikipedia.util.UriUtil
import java.util.regex.Pattern
import kotlin.math.abs

@Serializable
class MediaListItem {

    var title: String = ""

    @SerializedName("section_id")
    private val sectionId = 0
    val type: String = ""
    val caption: TextInfo? = null
    val showInGallery = false

    @SerializedName("srcset")
    val srcSets: List<@Contextual ImageSrcSet> = emptyList()

    constructor()
    constructor(title: String) {
        this.title = title
    }

    fun getImageUrl(preferredSize: Int): String {
        val pattern = Pattern.compile("/(\\d+)px-")
        var imageUrl = srcSets[0].src
        var lastSizeDistance = Int.MAX_VALUE
        srcSets.forEach { srcSet ->
            val matcher = pattern.matcher(srcSet.src)
            if (matcher.find() && matcher.group(1) != null) {
                val currentSizeDistance = abs(matcher.group(1).toInt() - preferredSize)
                if (currentSizeDistance < lastSizeDistance) {
                    imageUrl = srcSet.src
                    lastSizeDistance = currentSizeDistance
                }
            }
        }
        return UriUtil.resolveProtocolRelativeUrl(imageUrl)
    }

    fun getImageUrl(deviceScale: Float): String {
        var imageUrl = srcSets[0].src
        var lastScale = 1.0f
        srcSets.forEach { srcSet ->
            val scale = srcSet.getScale()
            if (deviceScale >= scale && lastScale < scale) {
                lastScale = scale
                imageUrl = srcSet.src
            }
        }
        return UriUtil.resolveProtocolRelativeUrl(imageUrl)
    }

    val isInCommons: Boolean
        get() = srcSets.isNotEmpty() && srcSets[0].src.contains(Service.URL_FRAGMENT_FROM_COMMONS)

    inner class ImageSrcSet {

        val src: String = ""
        private val scale: String? = null

        fun getScale(): Float {
            return scale?.replace("x", "")?.toFloat() ?: 0f
        }
    }
}
