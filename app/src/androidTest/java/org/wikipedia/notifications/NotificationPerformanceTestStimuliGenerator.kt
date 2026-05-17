package org.wikipedia.notifications

import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import java.time.Instant
import java.time.temporal.ChronoUnit

object NotificationPerformanceTestStimuliGenerator {

    fun generateNotifications(count: Int): List<Notification> {
        val baseTimestamp = Instant.parse("2025-01-01T00:00:00Z")
        return (1..count).map { i ->
            val timestampInstant = baseTimestamp.plus((i - 1).toLong(), ChronoUnit.HOURS)
            val timestamp = timestampInstant.toString()
            val read = if (i % 2 == 0) timestampInstant.plus(30, ChronoUnit.MINUTES).toString() else null
            
            createNotification(
                id = i.toLong(),
                wiki = if (i % 2 == 0) "en" else "zh", // toggle between two wikis
                category = when (i % 3) {              // inject different categories
                    0 -> "mention"
                    1 -> "edit-thank"
                    else -> "revert"
                },
                header = "Header $i",                   // each header is unique
                timestamp = timestamp,
                read = read,
                body = "Body of notification $i",       // each body is unique
                title = "Title $i",                     // each title is unique
                links = "Link $i"                       // each link is unique
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
                "wiki": "${wiki}wiki",
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
