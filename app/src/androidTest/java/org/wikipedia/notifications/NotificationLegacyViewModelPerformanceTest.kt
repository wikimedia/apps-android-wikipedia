package org.wikipedia.notifications

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class NotificationLegacyViewModelPerformanceTest {
    private val numberNotifications = 1000
    private val numberIterations = 100
    private val notificationRepository = FakeNotificationRepository()
    private val notificationPreferences = FakeNotificationPreferences()
    private val notificationFilterHelper = FakeNotificationFilterHelper()
    private lateinit var viewModel: NotificationLegacyViewModel

    @Before
    fun setUp() {
        runBlocking {
            viewModel = NotificationLegacyViewModel(
                notificationPreferences,
                notificationRepository,
                notificationFilterHelper
            )

            // Wait for view model to initialize without blocking the instrumentation thread
            withTimeout(10000) {
                viewModel.uiState.filter { it is Resource.Success }.first()
            }
        }
    }

    @Test
    fun testPerformance() = runBlocking {
        // Generate notifications
        val baseTimestamp = java.time.Instant.parse("2025-01-01T00:00:00Z")
        val notifications = (1..numberNotifications).map { i ->
            val timestampInstant =
                baseTimestamp.plus((i - 1).toLong(),
                    java.time.temporal.ChronoUnit.HOURS
                )
            val timestamp = timestampInstant.toString()
            val read =
                if (i % 2 == 0)
                    timestampInstant.plus(
                        30, java.time.temporal.ChronoUnit.MINUTES).toString()
                else null
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

        // Wait for initialization and initial fetch to finish without blocking
        withTimeout(10000) {
            viewModel.uiState.filter { it is Resource.Success && it.data.first.isNotEmpty() }.first()
        }

        val times = mutableListOf<Long>()

        println("Starting performance measurement ($numberNotifications notifications, $numberIterations iterations)...")

        repeat(numberIterations) { iteration ->
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
        val medianTime = if (numberIterations % 2 == 0) {
            (sortedTimes[numberIterations / 2 - 1] + sortedTimes[numberIterations / 2]) / 2.0
        } else {
            sortedTimes[numberIterations / 2].toDouble()
        }

        println("\nPerformance Result Summary ($numberNotifications notifications, $numberIterations iterations):")
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

        override suspend fun markItemsAsRead(
            ids: List<Long>,
            readTimestamp: String?
        ) {
        }

        override fun getNotificationsFlow(
            hideReadNotifications: Boolean,
            searchQuery: String?,
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>,
            hideNotMentioned: Boolean
        ): Flow<PagingData<Notification>> {
            return kotlinx.coroutines.flow.flowOf(PagingData.from(notifications))
        }

        override fun getUnreadCountsFlow(
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>
        ): Flow<Pair<Int, Int>> {
            return kotlinx.coroutines.flow.flowOf(0 to 0)
        }

        override suspend fun getRemoteKey(wiki: String): String? = null
        override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) {}
        override suspend fun clearRemoteKeys() {}
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
