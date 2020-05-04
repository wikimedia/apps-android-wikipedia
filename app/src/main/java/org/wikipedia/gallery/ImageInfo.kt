package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
@Suppress("UNUSED")
class ImageInfo : Serializable {
    val size = 0
    val width = 0
    val height = 0
    var source: String? = null
        get() = field ?: ""

    @SerializedName("thumburl")
    val thumbUrl: String? = null
        get() = field ?: ""

    @SerializedName("thumbwidth")
    private val thumbWidth = 0

    @SerializedName("thumbheight")
    private val thumbHeight = 0

    @SerializedName("url")
    val originalUrl: String? = null
        get() = field ?: ""

    @SerializedName("descriptionurl")
    private val descriptionUrl: String? = null

    @SerializedName("descriptionshorturl")
    private val descriptionShortUrl: String? = null

    @SerializedName("mime")
    val mimeType: String? = null
        get() = field ?: "*/*"

    @SerializedName("extmetadata")
    val metadata: ExtMetadata? = null
    val user: String? = null
        get() = field ?: ""
    val timestamp: String? = null
        get() = field ?: ""
    private val derivatives: List<Derivative>? = null
        get() = field ?: emptyList()
    var captions: Map<String, String>? = null
        get() = field ?: emptyMap()

    // Fields specific to video files:
    private val codecs: List<String>? = null
    private val name: String? = null

    @SerializedName("short_name")
    private val shortName: String? = null

    val commonsUrl: String
        get() = descriptionUrl ?: ""

    // TODO: make this smarter.
    val bestDerivative: Derivative?
        get() = derivatives?.last()

    class Derivative {
        val src: String? = null
            get() = field ?: ""
        private val type: String? = null
        private val title: String? = null
        private val shorttitle: String? = null
        private val width = 0
        private val height = 0
        private val bandwidth: Long = 0
    }
}
