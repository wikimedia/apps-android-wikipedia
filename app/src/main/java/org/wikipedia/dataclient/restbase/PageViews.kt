package org.wikipedia.dataclient.restbase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
@Suppress("unused")
class PageViews {
    val items: List<Item> = emptyList()

    @Serializable
    class Item {
        @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime? = null
        @SerialName("view_count") val viewCount: Long = 0
        @SerialName("rank_items") val rankItems: List<PageItem> = emptyList()
    }

    @Serializable
    class PageItem {
        val rank: Int = 0
        @SerialName("wiki_id") val wikiId: String = ""
        @SerialName("page_id") val pageId: Long = 0
        @SerialName("view_count") val viewCount: Long = 0
    }
}
