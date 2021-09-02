package org.wikipedia.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.notifications.Notification

@ExperimentalSerializationApi
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor =
        ContextualSerializer(Any::class, null, emptyArray()).descriptor

    override fun deserialize(decoder: Decoder): Any {
        val json = decoder as? JsonDecoder
            ?: throw IllegalStateException("Only JsonDecoder is supported.")
        return fromJson(json.decodeJsonElement())!!
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val json = encoder as? JsonEncoder
            ?: throw IllegalStateException("Only JsonEncoder is supported.")
        json.encodeJsonElement(toJson(value))
    }

    fun toJson(item: Any?): JsonElement = when(item) {
        null -> JsonNull
        is String -> JsonPrimitive(item)
        is Number -> JsonPrimitive(item)
        is Boolean -> JsonPrimitive(item)
        is UserContributionEvent -> Json.parseToJsonElement(Json.encodeToString(UserContributionEvent.serializer(),item))
        is Map<*, *> -> {
            val content = item.map { (k, v) -> k.toString() to toJson(v) }
            JsonObject(content.toMap())
        }
        is List<*> -> {
            val content = item.map { toJson(it) }
            JsonArray(content)
        }
        is JsonElement -> item
        else -> throw IllegalArgumentException("Unable to encode $item")
    }

    fun fromJson(item: JsonElement): Any? = when (item) {
        JsonNull -> null
        is JsonPrimitive -> when {
            item.isString -> item.content
            item.content == "true" || item.content == "false" -> {
                item.content == "true"
            }
            item.content.contains('.') -> item.content.toDouble()
            else -> item.content.toLong()
        }
        is JsonObject -> item.mapValues { fromJson(it.value) }
        is JsonArray -> item.map { fromJson(it) }
    }
}