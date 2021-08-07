package org.wikipedia.gallery

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
open class TextInfo(val html: String = "", val text: String = "", val lang: String = "") : Serializable

@JsonClass(generateAdapter = true)
class ArtistInfo(val name: String = "", @Json(name = "user_page") val userPage: String = "") : TextInfo()

/**
 * Moshi POJO for a standard image info object as returned by the API ImageInfo module
 */
@JsonClass(generateAdapter = true)
class ImageInfo(
    val size: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    var source: String = "",
    @Json(name = "thumburl") val thumbUrl: String = "",
    @Json(name = "thumbwidth") val thumbWidth: Int = 0,
    @Json(name = "thumbheight") val thumbHeight: Int = 0,
    @Json(name = "url") val originalUrl: String = "",
    @Json(name = "descriptionurl") val commonsUrl: String = "",
    @Json(name = "descriptionshorturl") internal val descriptionShortUrl: String = "",
    @Json(name = "mime") val mimeType: String = "*/*",
    @Json(name = "extmetadata") val metadata: ExtMetadata? = null,
    val user: String = "",
    val timestamp: String = "",
    val derivatives: List<Derivative> = emptyList(),
    var captions: Map<String, String> = emptyMap(),
    // Fields specific to video files:
    internal val codecs: List<String> = emptyList(),
    internal val name: String = "",
    @Json(name = "short_name") internal val shortName: String = ""
) : Serializable {
    // TODO: make this smarter.
    val bestDerivative: Derivative?
        get() = derivatives.lastOrNull()

    @JsonClass(generateAdapter = true)
    class Derivative(
        val src: String = "",
        internal val type: String = "",
        internal val title: String = "",
        @Json(name = "shorttitle") internal val shortTitle: String = "",
        internal val width: Int = 0,
        internal val height: Int = 0,
        internal val bandwidth: Long = 0
    )
}
