package org.wikipedia.gallery

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ImageInfo {

    var source = ""
    var captions = emptyMap<String, String>()

    @SerialName("short_name")
    private val shortName: String? = null

    @SerialName("descriptionshorturl")
    private val descriptionShortUrl: String? = null
    private val derivatives = emptyList<Derivative>()

    // Fields specific to video files:
    private val codecs: List<String>? = null
    private val name: String? = null

    @SerialName("descriptionurl")
    val commonsUrl = ""

    @SerialName("thumburl")
    val thumbUrl = ""

    @SerialName("thumbwidth")
    val thumbWidth = 0

    @SerialName("thumbheight")
    val thumbHeight = 0

    @SerialName("url")
    val originalUrl = ""

    val mime = "*/*"

    @SerializedName("extmetadata") @SerialName("extmetadata")
    val metadata: ExtMetadata? = null

    val user = ""
    val timestamp = ""
    val size = 0
    val width = 0
    val height = 0

    val bestDerivative get() = derivatives.lastOrNull()

    // TODO: make this smarter.
    @Serializable
    class Derivative {
        val src = ""
        private val type: String? = null
        private val title: String? = null
        private val shorttitle: String? = null
        private val width = 0
        private val height = 0
        private val bandwidth: Long = 0
    }
}
