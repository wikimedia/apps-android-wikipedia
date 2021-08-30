package org.wikipedia.gallery

import androidx.annotation.Nullable
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
class ImageInfo {

    var source = ""
    var captions = emptyMap<String, String>()

    @SerializedName("short_name")
    private val shortName: String? = null

    @SerializedName("descriptionshorturl")
    private val descriptionShortUrl: String? = null
    private val derivatives = emptyList<Derivative>()

    // Fields specific to video files:
    private val codecs: List<String>? = null
    private val name: String? = null

    @SerializedName("descriptionurl")
    val commonsUrl = ""

    @SerializedName("thumburl")
    val thumbUrl = ""

    @SerializedName("thumbwidth")
    val thumbWidth = 0

    @SerializedName("thumbheight")
    val thumbHeight = 0

    @SerializedName("url")
    val originalUrl = ""

    val mime = "*/*"

    @SerializedName("extmetadata")
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
