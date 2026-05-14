package org.wikipedia.notifications

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.util.Resource

@RunWith(RobolectricTestRunner::class)
class NotificationRefactoredViewModelTest {
    private lateinit var repository: FakeNotificationRepository
    private val preferences = FakeNotificationPreferences()
    private val notificationHelper = FakeNotificationFilterHelper()
    private val wikipediaApp = mockk<WikipediaApp>(relaxed = true)
    private lateinit var viewModel: NotificationRefactoredViewModel

    private lateinit var db: AppDatabase
    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FakeNotificationRepository(db.notificationDao())

        every { wikipediaApp.isOnline } returns true

        mockkObject(NotificationFilterActivity)
        every { NotificationFilterActivity.allWikisList() } returns listOf("en", "zh")
        every { NotificationFilterActivity.allTypesIdList() } returns listOf("edit-thank", "mention")

        viewModel = NotificationRefactoredViewModel(
            preferences,
            repository,
            notificationHelper
        )

        // Wait for view model to initialize by driving the looper
        var attempts = 0
        while (viewModel.uiState.value !is Resource.Success && attempts < 100) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10) // Give the background Room thread a moment to work
            attempts++
        }
    }

    @After
    fun tearDown() = runBlocking {
        val allNotifications = repository.getAllNotifications()
        //println("DATABASE DUMP: Found ${allNotifications.size} notifications")
        //allNotifications.forEach {
        //    println("[ID: ${it.id}] [Wiki: ${it.wiki}] [Read: ${it.read}] [Category: ${it.category}] [Header: ${it.contents?.header}]")
        //}
        withContext(Dispatchers.IO) {
            db.clearAllTables()
        }
        unmockkAll()
    }

    // helper function to wait for view model to execute Room database query
    fun waitForViewModel() {
        val maxAttempts = 100
        var attempts = 0
        val stateBefore = viewModel.uiState.value
        while (viewModel.uiState.value === stateBefore && attempts < maxAttempts) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10) // Give the background Room thread a moment to work
            attempts++
        }
        if (attempts < maxAttempts) {
            println("waitForViewModel: view model has processed after $attempts attempts.")
        }
        else {
            println("waitForViewModel: Aborted waiting for view model after $attempts attempts.")
        }
    }

    @Test
    fun testFilterAndPostNotifications() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "H1")
        val n2 = createNotification(2, "zh", "edit-thank", "H2")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.fetchAndSave()
        waitForViewModel()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is Resource.Success)
        assertEquals(2, (uiState as Resource.Success).data.first.size)
    }

    @Test
    fun testSorting() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Old", "2023-10-01T10:00:00Z")
        val n2 = createNotification(2, "en", "mention", "New", "2023-10-02T10:00:00Z")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.fetchAndSave()
        waitForViewModel()

        val successState = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val items = successState.data.first
        assertEquals(2L, items[0].notification?.id)
        assertEquals(1L, items[1].notification?.id)
    }

    @Test
    fun testSearchFiltering() = runBlocking {
        val n1 = createNotification(
            1,
            "en",
            "mention",
            "Apple",
            timestamp = "2023-10-03T10:00:00Z",
            body = "Orange",
            title = "Kiwi",
            links = "Melon"
        )
        val n2 = createNotification(
            2,
            "en",
            "mention",
            "Banana",
            timestamp = "2023-10-04T10:00:00Z",
            body = "Cherry",
            title = "Jabuticaba",
            links = "Passion fruit"
        )
        repository.insertNotifications(listOf(n1, n2))

        // check if match on header is reported
        viewModel.updateSearchQuery("app")
        waitForViewModel()

        val successStateHeaderFiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsHeaderFiltered = successStateHeaderFiltered.data.first
        assertEquals(1, itemsHeaderFiltered.size)
        assertEquals(1L, itemsHeaderFiltered[0].notification?.id)

        // check if match on body is reported
        viewModel.updateSearchQuery("ora")
        waitForViewModel()

        val successStateBodyFiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsBodyFiltered = successStateBodyFiltered.data.first
        assertEquals(1, itemsBodyFiltered.size)
        assertEquals(1L, itemsBodyFiltered[0].notification?.id)

        // check if match on title is reported
        viewModel.updateSearchQuery("kiw")
        waitForViewModel()

        val successStateTitleFiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsTitleFiltered = successStateTitleFiltered.data.first
        assertEquals(1, itemsTitleFiltered.size)
        assertEquals(1L, itemsTitleFiltered[0].notification?.id)

        // check if match on links is reported
        viewModel.updateSearchQuery("mel")
        waitForViewModel()

        val successStateLinkFiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsLinkFiltered = successStateLinkFiltered.data.first
        assertEquals(1, itemsLinkFiltered.size)
        assertEquals(1L, itemsLinkFiltered[0].notification?.id)
    }

    @Test
    fun testHideReadFiltering() = runBlocking {
        val n1 = createNotification(
            1,
            "en",
            "mention",
            "Unread",
            read = "",
            timestamp = "2023-10-27T10:45:00.123Z"
        )
        n1.read = null
        val n2 = createNotification(
            2,
            "en",
            "mention",
            "Read",
            read = "2023-10-27T14:45:00.123Z",
            timestamp = "2023-10-27T11:45:00.123Z"
        )
        n2.read = "2023-10-01T10:00:00Z" // mark the notification as read by writing a timestamp
        repository.insertNotifications(listOf(n1, n2)) // update the fake
        preferences.excludedTypes.add("some-dummy-type")
        preferences.hideRead = true // set the fake preference to filter out read notifications
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()
        val successState = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsFiltered = successState.data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        preferences.hideRead = false // set the fake preference to not filter out read notifications
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()
        val uiStateUnfiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsUnfiltered = uiStateUnfiltered.data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(2L, itemsUnfiltered[0].notification?.id)
        assertEquals(1L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testWikiFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "zh", "mention", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedWikis.add("en") // add "en" to the list of wikis to be filtered out
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()
        val successStateFiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsFiltered = successStateFiltered.data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(2L, itemsFiltered[0].notification?.id)

        preferences.excludedWikis.remove("en") // remove "en" from the list
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()

        val successStateUnfiltered = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsUnfiltered = successStateUnfiltered.data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testTypeFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "en", "edit-thank", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedTypes.add("edit-thank") // add "edit-thank" to the list of types to be filtered out
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()

        val successState = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsFiltered = successState.data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        preferences.excludedTypes.remove("edit-thank") // remove "edit-thank" from the list
        viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
        waitForViewModel()

        val uiStateUnfiltered = viewModel.uiState.value
        val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
        assertEquals(2, itemsUnfiltered.size)
        assertEquals(1L, itemsUnfiltered[0].notification?.id)
        assertEquals(2L, itemsUnfiltered[1].notification?.id)
    }

    @Test
    fun testTabFiltering() = runBlocking {
        preferences.excludedTypes.add("dummy")
        val n1 = createNotification(1, "en", "mention", "Mention")
        val n2 = createNotification(2, "en", "edit-thank", "Not Mention")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.updateTabSelection(1) // Simulate selection of "Mentions" tab
        waitForViewModel()

        val successState = viewModel.uiState.filter { it is Resource.Success }.first() as Resource.Success
        val itemsFiltered = successState.data.first
        assertEquals(1, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[0].notification?.id)

        viewModel.updateTabSelection(0) // Simulate selection of "All" tab
        waitForViewModel()

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
        timestamp: String = "2022-10-27T14:45:00.123Z",
        read: String = "",
        title: String = "",
        body: String = "",
        links: String = "",
    ): Notification {
        val json = $$"""
            {
                "id": $$id,
                "wiki": "$$wiki",
                "category": "$$category",
                "title": {
                    "full": "$$title",
                    "text": "$$title"
                },
                "read": "$$read",
                "timestamp": {
                    "utciso8601": "$$timestamp"
                },
                "*": {
                    "header": "$$header",
                    "body": "$$body",
                    "links": {
                        "secondary": [
                            { "label": "$$links", "url": "" }
                        ]
                    }
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
    private class FakeNotificationRepository(private val notificationDao: NotificationDao) : NotificationRepository {
        var unreadWikis = mapOf<String, WikiSite>()

        suspend fun insertNotifications(notifications: List<Notification>) {
            notificationDao.insertNotifications(notifications)
        }
        override suspend fun getAllNotifications(): List<Notification> {
            return notificationDao.getAllNotifications()
        }
        override suspend fun updateNotification(notification: Notification) {
            notificationDao.updateNotification(notification)
        }
        override suspend fun fetchUnreadWikiDbNames() = unreadWikis
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = null
        override suspend fun getAllSelectedNotifications(
            hideReadNotifications: Boolean,
            searchQuery: String?,
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>,
            hideNotMentioned: Boolean
        ): List<Notification> {
            return notificationDao.getAllSelectedNotification(
                hideReadNotifications,
                searchQuery,
                hasExclusions = !excludedTypeCodes.isEmpty(),
                excludedTypeCodes,
                includedWikiCodes,
                hideNotMentioned,
                NotificationCategory.MENTIONS_GROUP.map { it.id }
            )
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