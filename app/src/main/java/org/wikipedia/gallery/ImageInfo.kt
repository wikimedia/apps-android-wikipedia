package org.wikipedia.gallery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.JsonUtil

@Suppress("unused")
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

    @SerialName("metadata")
    val plainMetadata: List<MetadataItem> = emptyList()

    @SerialName("extmetadata")
    val metadata: ExtMetadata? = null

    val user = ""
    val timestamp = ""
    val size = 0
    val width = 0
    val height = 0

    fun getBestDerivativeForSize(widthDp: Int): Derivative? {
        var derivative: Derivative? = null
        derivatives.forEach {
            if (it.width in 1..<widthDp) {
                if ((derivative == null || it.width > derivative.width) && !it.type.contains("ogg") && !it.type.contains("ogv")) {
                    derivative = it
                }
            }
        }
        return derivative
    }

    fun getMetadataTranslations(): List<String> {
        plainMetadata.firstOrNull { it.name == "translations" }?.let { meta ->
            if (meta.value is JsonArray) {
                val translations = JsonUtil.json.decodeFromJsonElement<List<MetadataItem>>(meta.value)
                return translations.map { it.name }
            }
        }
        return emptyList()
    }

    @Serializable
    class Derivative {
        val src = ""
        val type = ""
        private val title: String? = null
        private val shorttitle: String? = null
        val width = 0
        private val height = 0
        private val bandwidth: Long = 0
    }

    @Serializable
    class MetadataItem {
        val name = ""
        val value: JsonElement? = null
    }
}
