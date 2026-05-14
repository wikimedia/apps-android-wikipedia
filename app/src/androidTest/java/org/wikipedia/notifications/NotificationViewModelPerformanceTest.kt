package org.wikipedia.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class NotificationViewModelPerformanceTest {
    private val notificationRepository = FakeNotificationRepository()
    private val notificationPreferences = FakeNotificationPreferences()
    private val notificationFilterHelper = FakeNotificationFilterHelper()
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setUp() {
        // Initialize ViewModel with fakes.
        // Note: init block will trigger an initial load, but repository is empty then.
        viewModel = NotificationViewModel(
            notificationPreferences,
            notificationRepository,
            notificationFilterHelper
        )
    }

    @Test
    fun testPerformance() = runBlocking {
        // Generate notifications
        val numberNotifications = 100
        val baseTimestamp = java.time.Instant.parse("2025-01-01T00:00:00Z")
        val notifications = (1..numberNotifications).map { i ->
            val timestampInstant = baseTimestamp.plus((i - 1).toLong(), java.time.temporal.ChronoUnit.HOURS)
            val timestamp = timestampInstant.toString()
            val read = if (i % 2 == 0) timestampInstant.plus(30, java.time.temporal.ChronoUnit.MINUTES).toString() else null
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
        println("Adding ${notifications.size} notifications to mock repository...")
        notificationRepository.notifications.addAll(notifications)

        // Apply a combination of filters
        notificationPreferences.hideRead = true
        notificationPreferences.excludedWikis.add("en")
        notificationPreferences.excludedTypes.add("type1")
        viewModel.updateSearchQuery("Header 1") // Will limit to headers starting with "Header 1"

        // Perform an initial fetch to ensure uiState is populated with Success data
        // which simplifies the drop(1) logic in the measurement loop.
        viewModel.fetchAndSave(refresh = true)
        viewModel.uiState.filter { it is Resource.Success && it.data.first.isNotEmpty() }.first()

        val iterations = 10
        val times = mutableListOf<Long>()

        println("Starting performance measurement ($numberNotifications notifications, $iterations iterations)...")

        repeat(iterations) { iteration ->
            val time = measureTimeMillis {
                viewModel.fetchAndSave(refresh = true)
                // Use drop(1) to ensure we wait for the NEW emission triggered by fetchAndSave
                // rather than returning the current StateFlow value immediately.
                viewModel.uiState.filter { it is Resource.Success && it.data.first.isNotEmpty() }
                    .drop(1)
                    .first()
            }
            times.add(time)
            println("Iteration ${iteration + 1}: $time ms")
        }

        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0
        val averageTime = times.average()
        val sortedTimes = times.sorted()
        val medianTime = (sortedTimes[4] + sortedTimes[5]) / 2.0

        println("\nPerformance Result Summary ($numberNotifications notifications, $iterations iterations):")
        println("Min: $minTime ms")
        println("Max: $maxTime ms")
        println("Average: ${"%.2f".format(averageTime)} ms")
        println("Median: ${"%.2f".format(medianTime)} ms")

        // Log count of items for verification
        val finalResource = viewModel.uiState.value as Resource.Success
        println("Final item count in UI: ${finalResource.data.first.size}")
    }

    private fun createNotification(
        id: Long,
        wiki: String,
        category: String,
        header: String,
        timestamp: String = "2023-10-27T10:00:00Z",
        read: String? = null,
        title: String = "",
        body: String = "",
        links: String = ""
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

    private class FakeNotificationPreferences : NotificationPreferences {
        var hideRead = false
        val excludedTypes = mutableSetOf<String>()
        val excludedWikis = mutableSetOf<String>()

        override fun isHideReadNotificationsEnabled() = hideRead
        override fun getNotificationExcludedTypeCodes() = excludedTypes
        override fun getNotificationExcludedWikiCodes() = excludedWikis
    }

    private class FakeNotificationRepository : NotificationRepository {
        val notifications = mutableListOf<Notification>()
        override suspend fun getAllNotifications() = notifications
        override suspend fun updateNotification(notification: Notification) {}
        override suspend fun fetchUnreadWikiDbNames() = emptyMap<String, WikiSite>()
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = null
        override suspend fun getAllSelectedNotifications(
            hideReadNotifications: Boolean,
            searchQuery: String?,
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>,
            hideNotMentioned: Boolean
        ): List<Notification> {
            return emptyList()
        }
    }

    private class FakeNotificationFilterHelper: NotificationFilterHelper {
        override fun allWikisList(): List<String> {
            return listOf("en", "zh", "dummy")
        }

        override fun allTypesIdList(): List<String> {
            return listOf("type1", "type2")
        }
    }
}
