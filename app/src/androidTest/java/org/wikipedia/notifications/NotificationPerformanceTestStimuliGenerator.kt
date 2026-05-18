package org.wikipedia.notifications

import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import java.time.Instant
import java.time.temporal.ChronoUnit

object NotificationPerformanceTestStimuliGenerator {

    private val ALL_CATEGORIES = listOf(
        "system", "thank-you-edit", "edit-user-talk", "edit-thank", "reverted",
        "login-fail", "mention", "emailuser", "user-rights", "article-linked",
        "alpha-builder-checker", "reading-list-syncing", "syncing", "recommended-reading-lists", "games",
        "type1" // Added to exercise the type exclusion filter used in the performance test
    )

    private val WIKI_FORMATS = listOf("%swiki", "%s_wiki")

    fun generateNotifications(count: Int): List<Notification> {
        val baseTimestamp = Instant.parse("2025-01-01T00:00:00Z")
        
        // Large noise string to increase computational cost of SQL string matching (LIKE operations)
        val noise = "Dear Brutus, the fault is not in our stars but in ourselves that we are underlings."

        return (1..count).map { i ->
            val timestampInstant = baseTimestamp.plus((i - 1).toLong(), ChronoUnit.HOURS)
            
            // lang: even=en (excluded by test), odd=zh (included by test)
            val langCode = if (i % 2 == 0) "en" else "zh"
            
            // format: index 0 = zhwiki (matches "zh" in SQL), index 1 = zh_wiki (matches "zh-" in SQL)
            // We use (i/2) % 2 to rotate format independently of langCode to ensure some zh items use %swiki.
            val wikiName = WIKI_FORMATS[(i / 2) % WIKI_FORMATS.size].format(langCode)
            
            // Cycle through categories to exercise every branch of the 15-way SQL filter
            val category = ALL_CATEGORIES[i % ALL_CATEGORIES.size]
            
            createNotification(
                id = i.toLong(),
                wiki = wikiName,
                category = category,
                header = "Header $i: $noise",
                timestamp = timestampInstant.toString(),
                // read: every 4th is read (filtered out by test if hideRead=true)
                read = if (i % 4 == 0) timestampInstant.plus(30, ChronoUnit.MINUTES).toString() else null,
                body = "Body $i: $noise",
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
