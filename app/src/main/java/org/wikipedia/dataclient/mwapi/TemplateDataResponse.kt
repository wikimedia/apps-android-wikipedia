package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.json.JsonUtil
import org.wikipedia.util.log.L

@Serializable
class TemplateDataResponse : MwResponse() {

    private val pages: Map<String, TemplateData>? = null

    val getTemplateData get() = pages?.values?.toList() ?: emptyList()

    @Serializable
    class TemplateData {
        val title: String = ""
        // When send lang=[langCode], the type of it will become String instead of a Map<String, String>
        val description: String? = null
        private val params: JsonElement? = null
        val format: String? = null
        @SerialName("notemplatedata") val noTemplateData: Boolean = false

        val getParams: Map<String, TemplateDataParam>? get() {
            try {
                if (params != null && params !is JsonArray) {
                    return if (noTemplateData) {
                        JsonUtil.json.decodeFromJsonElement<Map<String, JsonArray>>(params).mapValues {
                            TemplateDataParam()
                        }
                    } else {
                        JsonUtil.json.decodeFromJsonElement<Map<String, TemplateDataParam>>(params)
                    }
                }
            } catch (e: Exception) {
                L.d("Error on parsing params $e")
            }
            return null
        }
    }

    @Serializable
    class TemplateDataParam {
        // [label, description, default and example]: The original format of them is in a Map style;
        // When you send a target language in the request, it will become a String.
        val label: String? = null
        val description: String? = null
        val default: String? = null
        val example: String? = null
        val type: String = ""
        val required: Boolean = false
        val suggested: Boolean = false
        @SerialName("autovalue") val autoValue: String? = null
        @SerialName("suggestedvalues") val suggestedValues: List<String> = emptyList()
        val aliases: List<String> = emptyList()
        private val deprecated: JsonElement? = null

        private val deprecatedAsString get() = deprecated?.jsonPrimitive?.contentOrNull

        val isDeprecated get() = deprecatedAsString.equals("true", true)
    }
}
