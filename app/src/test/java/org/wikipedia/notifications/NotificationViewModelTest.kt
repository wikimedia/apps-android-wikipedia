package org.wikipedia.notifications

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource

@RunWith(RobolectricTestRunner::class)
class NotificationViewModelTest {
    private val repository = FakeNotificationRepository()
    private val preferences = FakeNotificationPreferences()
    private val wikipediaApp = mockk<WikipediaApp>(relaxed = true)
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setUp() {
        mockkObject(WikipediaApp)
        every { WikipediaApp.instance } returns wikipediaApp
        every { wikipediaApp.isOnline } returns true

        mockkObject(NotificationFilterActivity)
        every { NotificationFilterActivity.allWikisList() } returns listOf("en", "zh")
        every { NotificationFilterActivity.allTypesIdList() } returns listOf("edit-thank", "mention")

        viewModel = NotificationViewModel(preferences, repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testFilterAndPostNotifications() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "H1")
        val n2 = createNotification(2, "zh", "edit-thank", "H2")
        repository.notifications.addAll(listOf(n1, n2))

        viewModel.fetchAndSave()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is Resource.Success)
        assertEquals(2, (uiState as Resource.Success).data.first.size)
    }

    @Test
    fun testSorting() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Old", "2023-10-01T10:00:00Z")
        val n2 = createNotification(2, "en", "mention", "New", "2023-10-02T10:00:00Z")
        repository.notifications.addAll(listOf(n1, n2))

        viewModel.fetchAndSave()

        val uiState = viewModel.uiState.value
        val items = (uiState as Resource.Success).data.first
        assertEquals(2L, items[0].notification?.id)
        assertEquals(1L, items[1].notification?.id)
    }

    @Test
    fun testSearchFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Apple")
        val n2 = createNotification(2, "en", "mention", "Banana")
        repository.notifications.addAll(listOf(n1, n2))

        viewModel.updateSearchQuery("App")

        val uiState = viewModel.uiState.value
        val items = (uiState as Resource.Success).data.first
        assertEquals(1, items.size)
        assertEquals(1L, items[0].notification?.id)
    }

    @Test
    fun testHideReadFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Unread")
        val n2 = createNotification(2, "en", "mention", "Read")
        n2.read = "2023-10-01T10:00:00Z" // mark the notification as read by writing a timestamp
        repository.notifications.addAll(listOf(n1, n2)) // update the fake

        preferences.hideRead = true // set the fake preference to filter out read notifications
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateFiltered = viewModel.uiState.value
        val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        preferences.hideRead = false // set the fake preference to not filter out read notifications
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateUnfiltered = viewModel.uiState.value
        val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testWikiFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "zh", "mention", "Excluded")
        repository.notifications.addAll(listOf(n1, n2))

        preferences.excludedWikis.add("en") // add "en" to the list of wikis to be filtered out
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateFiltered = viewModel.uiState.value
        val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(2L, itemsFiltered[0].notification?.id)

        preferences.excludedWikis.remove("en") // remove "en" from the list
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateUnfiltered = viewModel.uiState.value
        val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testTypeFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "en", "edit-thank", "Excluded")
        repository.notifications.addAll(listOf(n1, n2))

        preferences.excludedTypes.add("edit-thank") // add "edit-thank" to the list of types to be filtered out
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateFiltered = viewModel.uiState.value
        val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        preferences.excludedTypes.remove("edit-thank") // remove "edit-thank" from the list
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState

        val uiStateUnfiltered = viewModel.uiState.value
        val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testTabFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Mention")
        val n2 = createNotification(2, "en", "edit-thank", "Not Mention")
        repository.notifications.addAll(listOf(n1, n2))

        viewModel.updateTabSelection(1) // Simulate selection of "Mentions" tab

        val uiStateFiltered = viewModel.uiState.value
        val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        viewModel.updateTabSelection(0) // Simulate selection of "All" tab
        val uiStateUnfiltered = viewModel.uiState.value
        val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    private fun createNotification(
        id: Long,
        wiki: String,
        category: String,
        header: String,
        timestamp: String = "2023-10-27T10:00:00Z"
    ): Notification {
        val json = """
            {
                "id": $id,
                "wiki": "$wiki",
                "category": "$category",
                "timestamp": {
                    "utciso8601": "$timestamp"
                },
                "*": {
                    "header": "$header"
                }
            }
        """.trimIndent()
        return JsonUtil.decodeFromString<Notification>(json)!!
    }

    // Fake notification preferences used for mocking during test
    private class FakeNotificationPreferences : NotificationPreferences {
        var hideRead = false
        val excludedTypes = mutableSetOf<String>()
        val excludedWikis = mutableSetOf<String>()

        override fun isHideReadNotificationsEnabled() = hideRead
        override fun getNotificationExcludedTypeCodes() = excludedTypes
        override fun getNotificationExcludedWikiCodes() = excludedWikis
    }

    // Fake notification repository used for mocking during test
    private class FakeNotificationRepository : NotificationRepository {
        val notifications = mutableListOf<Notification>()
        var unreadWikis = mapOf<String, WikiSite>()

        override suspend fun getAllNotifications() = notifications
        override suspend fun updateNotification(notification: Notification) {
            val index = notifications.indexOfFirst { it.id == notification.id && it.wiki == notification.wiki }
            if (index != -1) {
                notifications[index] = notification
            }
        }
        override suspend fun fetchUnreadWikiDbNames() = unreadWikis
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = null
    }
}
