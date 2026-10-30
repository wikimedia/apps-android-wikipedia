package org.wikipedia.topics.db

import androidx.room.Entity
import androidx.room.Index
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageMetadata

@Entity(
    primaryKeys = ["lang", "namespace", "apiTitle", "topic"],
    indices = [Index(value = ["lang", "namespace", "apiTitle"]), Index(value = ["topic"])]
)
data class PageTopic(
    val lang: String,
    val namespace: String,
    val apiTitle: String,
    val topic: String,
    val score: Double = 0.0
) {
    companion object {
        const val MAX_TOPICS_PER_PAGE = 5

        fun fromMetadata(entry: HistoryEntry, topics: List<PageMetadata.Topic>): List<PageTopic> {
            return topics
                .filter { !it.topic.isNullOrEmpty() }
                .sortedByDescending { it.score }
                .distinctBy { it.topic }
                .take(MAX_TOPICS_PER_PAGE)
                .map { PageTopic(entry.lang, entry.namespace, entry.apiTitle, it.topic.orEmpty(), it.score) }
        }
    }
}
