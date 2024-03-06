package org.wikipedia.dataclient.mwapi

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
        val description: Map<String, String>? = null
        val params: Map<String, TemplateDataParam>? = null
        val format: String? = null
    }

    @Serializable
    class TemplateDataParam {
        val label: Map<String, String>? = null
        val description: Map<String, String>? = null
        val type: String = ""
        val required: Boolean = false
        val aliases: List<String> = emptyList()
        val example: Map<String, String>? = null
        val suggested: Boolean = false
        private val deprecated: JsonElement? = null
        val autovalue: String? = null
        val default: String? = null
        val suggestedvalues: List<String> = emptyList()

        val deprecatedAsBoolean get() = deprecated?.jsonPrimitive?.booleanOrNull ?: false
        val deprecatedAsString get() = deprecated?.jsonPrimitive?.contentOrNull
    }
}
