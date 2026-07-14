package org.wikimedia.testkitchen.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
open class Sampleable {
    @SerialName("sample_rate") private val _sampleRate: Map<JsonPrimitive, JsonElement> = emptyMap()

    fun sampleRate(wiki: String): Float {
        return sampleRateByWiki[wiki] ?: defaultSampleRate
    }

    private var defaultSampleRate: Float = 1.0f
    private val sampleRateByWiki: MutableMap<String, Float> = mutableMapOf()

    init {
        _sampleRate.forEach { (k, v) ->
            if (k.isString && k.content == "default" && v is JsonPrimitive) {
                defaultSampleRate = v.jsonPrimitive.floatOrNull ?: 1.0f
            } else if (k.floatOrNull != null && v is JsonArray) {
                v.jsonArray.forEach {
                    if (it.jsonPrimitive.isString) {
                        sampleRateByWiki[it.jsonPrimitive.content] = k.float
                    }
                }
            }
        }
    }
}
