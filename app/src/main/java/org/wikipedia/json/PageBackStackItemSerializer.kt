package org.wikipedia.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.page.tabs.PageBackStackItem

// TODO: remove on 2026-07-01
object PageBackStackItemSerializer : KSerializer<PageBackStackItem> {
    override val descriptor = buildClassSerialDescriptor("PageBackStackItem")

    override fun deserialize(decoder: Decoder): PageBackStackItem {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement().jsonObject

        // This is to handle the old format of PageBackStackItem which originally stored in the Prefs.tabs
        return if (element.containsKey("title") && element.containsKey("historyEntry")) {
            val oldFormat = Json.decodeFromJsonElement<OldPageBackStackItem>(element)
            PageBackStackItem(oldFormat.title, oldFormat.historyEntry).also {
                it.scrollY = oldFormat.scrollY
            }
        } else {
            Json.decodeFromJsonElement<PageBackStackItem>(element)
        }
    }

    override fun serialize(encoder: Encoder, value: PageBackStackItem) {
        encoder.encodeSerializableValue(PageBackStackItem.serializer(), value)
    }
}

@Serializable
private data class OldPageBackStackItem(
    val title: PageTitle,
    val historyEntry: HistoryEntry,
    val scrollY: Int = 0
)
