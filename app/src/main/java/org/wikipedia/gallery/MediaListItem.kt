package org.wikipedia.gallery

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.Service
import org.wikipedia.util.UriUtil
import java.io.Serializable
import java.util.regex.Pattern
import kotlin.math.abs

@JsonClass(generateAdapter = true)
class MediaListItem(
    val title: String = "",
    @Json(name = "section_id") val sectionId: Int = 0,
    val type: String = "",
    val caption: TextInfo? = null,
    @Json(name = "showInGallery") val isShowInGallery: Boolean = false,
    @Json(name = "srcset") val srcSets: List<ImageSrcSet> = emptyList()
) : Serializable {
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
            val scale = srcSet.scale
            if (deviceScale >= scale && lastScale < scale) {
                lastScale = scale
                imageUrl = srcSet.src
            }
        }
        return UriUtil.resolveProtocolRelativeUrl(imageUrl)
    }

    val isInCommons: Boolean
        get() = srcSets.isNotEmpty() && srcSets[0].src.contains(Service.URL_FRAGMENT_FROM_COMMONS)

    @JsonClass(generateAdapter = true)
    inner class ImageSrcSet(val src: String = "", @Json(name = "scale") internal val scaleStr: String = "") : Serializable {
        val scale: Float
            get() = scaleStr.replace("x", "").toFloat()
}
