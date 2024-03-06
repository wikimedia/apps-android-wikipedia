package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
class TemplateDataResponse : MwResponse() {

    val pages: Map<String, TemplateData>? = null

    @Serializable
    class TemplateData {
        val title: String = ""
        // When send lang=[langCode], the type of it will become String instead of a Map<String, String>
        val description: String? = null
        val params: Map<String, TemplateDataParam>? = null
        val format: String? = null
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

        val deprecatedAsBoolean get() = deprecated?.jsonPrimitive?.booleanOrNull ?: false
        val deprecatedAsString get() = deprecated?.jsonPrimitive?.contentOrNull
    }
}
