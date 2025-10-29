package org.wikipedia.dataclient.restbase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
@Suppress("unused")
class UserEdits {
    val items: List<Item> = emptyList()

    @Serializable
    class Item {
        @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime? = null
        @SerialName("edit_count") val editCount: Int = 0
    }
}
