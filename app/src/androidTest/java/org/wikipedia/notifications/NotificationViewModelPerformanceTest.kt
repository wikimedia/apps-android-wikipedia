package org.wikipedia.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.wikipedia.util.StringUtil
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class NotificationViewModelPerformanceTest {
    private val repository = FakeNotificationRepository()
    private val preferences = FakeNotificationPreferences()
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setUp() {
        // Initialize ViewModel with fakes.
        // Note: init block will trigger an initial load, but repository is empty then.
        viewModel = NotificationViewModel(
            preferences,
            repository,
            FakeNotificationFilterHelper()
        )
    }

    @Test
    fun testPerformanceWith1000Notifications() = runBlocking {
        // Generate 1000 notifications
        val notifications = (1..1000).map { i ->
            createNotification(
                id = i.toLong(),
                wiki = if (i % 2 == 0) "en" else "zh", // toggle between two wikis
                category = when (i % 3) {              // inject different categories
                    0 -> "mention"
                    1 -> "edit-thank"
                    else -> "revert"
                },
                header = "Header $i",                   // each header is unique
                body = "Body of notification $i",       // each body is unique
                title = "Title $i",                     // each title is unique
                links = "Link $i"                       // each link is unique
            )
        }
        repository.notifications.addAll(notifications)

        // Apply a combination of filters
        preferences.hideRead = true
        
        preferences.excludedWikis.add("en")

        preferences.excludedTypes.add("type1")

        viewModel.updateSearchQuery("Header 1") // Will limit to headers starting with "Header 1"

        // Measure the time for fetchAndSave(refresh = true) which includes:
        // 1. Repository fetch (mocked, no-op)
        // 2. getAllNotifications from repository (reading 1000 items)
        // 3. processList (The core logic: deduplication, sorting, filtering 1000 items)
        // 4. Posting to uiState
        
        val time = measureTimeMillis {
            viewModel.fetchAndSave(refresh = true)
            // Wait for the Success state to be posted to uiState Flow
            viewModel.uiState.filter { it is Resource.Success && it.data.first.isNotEmpty() }.first()
        }

        val status = when (
            val resource = viewModel.uiState.value
        ) {
            is Resource.Success -> {
                resource.data.first[0].notification?.let {
                    val firstEntry = StringUtil.dbNameToLangCode(it.wiki)
                    println("First entry: $firstEntry")
                }
                resource.data.first[1].notification?.let {
                    val secondEntry = StringUtil.dbNameToLangCode(it.wiki)
                    println("Second entry: $secondEntry")
                }
                val numberItems = resource.data.first.size
                "($numberItems items)"
            }
            else -> {
                "(something went wrong)"
            }
        }

        println("PerformanceResult: fetchAndSave with 1000 notifications took $time ms $status")
    }

    private fun createNotification(
        id: Long,
        wiki: String,
        category: String,
        header: String,
        timestamp: String = "2023-10-27T10:00:00Z",
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
