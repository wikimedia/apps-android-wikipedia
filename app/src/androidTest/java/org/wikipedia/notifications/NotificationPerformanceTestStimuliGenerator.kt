package org.wikipedia.notifications

import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import java.time.Instant
import java.time.temporal.ChronoUnit

object NotificationPerformanceTestStimuliGenerator {

    private val ALL_CATEGORIES = listOf(
        "system", "thank-you-edit", "edit-user-talk", "edit-thank", "reverted",
        "login-fail", "mention", "emailuser", "user-rights", "article-linked",
        "alpha-builder-checker", "reading-list-syncing", "syncing", "recommended-reading-lists", "games"
    )

    private val WIKI_FORMATS = listOf("%swiki", "%s_wiki")

    fun generateNotifications(count: Int): List<Notification> {
        val baseTimestamp = Instant.parse("2025-01-01T00:00:00Z")
        
        val noise = "Dear Brutus, the fault is not in our stars."
        return (1..count).map { i ->
            val timestampInstant = baseTimestamp.plus((i - 1).toLong(), ChronoUnit.HOURS)
            val langCode = if (i % 2 == 0) "en" else "zh"
            val wikiName = WIKI_FORMATS[i % WIKI_FORMATS.size].format(langCode)
            
            // Cycle through ALL categories to exercise every branch of the 15-way NOT LIKE filter
            val category = ALL_CATEGORIES[i % ALL_CATEGORIES.size]
            
            createNotification(
                id = i.toLong(),
                wiki = wikiName,
                category = category,
                header = "Header $i: $noise",
                timestamp = timestampInstant.toString(),
                read = if (i % 4 == 0) timestampInstant.plus(30, ChronoUnit.MINUTES).toString() else null,
                body = "Body $i: ${noise.repeat(10)}",
                title = "Title $i: $noise",
                links = "Link $i"
            )
        }
    }

    private fun createNotification(
        id: Long,
        wiki: String,
        category: String,
        header: String,
        timestamp: String,
        read: String?,
        title: String,
        body: String,
        links: String
    ): Notification {
        val json = """
            {
                "id": $id,
                "wiki": "$wiki",
                "category": "$category",
                "title": {
                    "full": "$title",
                    "text": "$title"
                },
                "read": ${if (read == null) "null" else "\"$read\""},
                "timestamp": {
                    "utciso8601": "$timestamp"
                },
                "*": {
                    "header": "$header",
                    "body": "$body",
                    "links": {
                        "secondary": [
                            { "label": "$links", "url": "http://test.com" }
                        ]
                    }
                }
            }
        """.trimIndent()
        return JsonUtil.decodeFromString<Notification>(json)!!
    }
}
