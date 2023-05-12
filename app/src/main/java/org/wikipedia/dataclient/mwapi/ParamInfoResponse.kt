package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Suppress("unused")
@Serializable
class ParamInfoResponse : MwResponse() {

    val paraminfo: ParamInfo? = null

    @Serializable
    class ParamInfo {
        val modules: List<Module> = emptyList()
    }

    @Serializable
    class Module {
        val name = ""
        val classname = ""
        val path = ""
        val source = ""
        val parameters: List<Parameter> = emptyList()
    }

    @Serializable
    class Parameter {
        val index = 0
        val name = ""
        val required = false
        val multi = false
        val type: JsonElement? = null

        val typeAsString get() = type?.jsonPrimitive?.content.orEmpty()
        val typeAsEnum get() = type?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    }
}
