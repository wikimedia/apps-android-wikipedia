package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

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
        val deprecated: Boolean = false // TODO: It will be string if is not deprecated
        val autovalue: String? = null
        val default: String? = null
        val suggestedvalues: List<String> = emptyList()
    }
}
