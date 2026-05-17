package org.wikipedia.notifications

import android.view.ViewGroup
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
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
        mockkObject(WikipediaApp)
        every { WikipediaApp.instance } returns wikipediaApp
        every { wikipediaApp.isOnline } returns true

        val context = org.robolectric.RuntimeEnvironment.getApplication()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FakeNotificationRepository(db.notificationDao())

        mockkObject(NotificationFilterActivity)
        every { NotificationFilterActivity.allWikisList() } returns listOf("en", "zh")
        every { NotificationFilterActivity.allTypesIdList() } returns listOf("edit-thank", "mention")

        viewModel = NotificationRefactoredViewModel(
            preferences,
            repository,
            notificationHelper
        )

        // Wait for view model to initialize
        waitForViewModel()

        runBlocking {
            withContext(Dispatchers.IO) {
                db.notificationDao().deleteAll()
            }
        }
    }

    @After
    fun tearDown() = runBlocking {
        withContext(Dispatchers.IO) {
            db.close()
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
    }

    private suspend fun collectPagingData(): List<NotificationListItemContainer> = kotlinx.coroutines.coroutineScope {
        val adapter = NotificationItemAdapter()
        val job = launch {
            viewModel.notificationFlow.collectLatest {
                adapter.submitData(it)
            }
        }
        
        // Wait for Paging to finish initial load by idling the looper
        var attempts = 0
        while (adapter.itemCount == 0 && attempts < 200) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            kotlinx.coroutines.delay(20)
            attempts++
        }
        val items = adapter.snapshot().items
        job.cancel()
        items
    }

    @Test
    fun testFilterAndPostNotifications() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "H1")
        val n2 = createNotification(2, "zh", "edit-thank", "H2")
        repository.insertNotifications(listOf(n1, n2))

        val items = collectPagingData()
        // 2 items + 1 search bar header
        assertEquals(3, items.size)
        assertEquals(1L, items[1].notification?.id)
    }

    @Test
    fun testSorting() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Old", "2023-10-01T10:00:00Z")
        val n2 = createNotification(2, "en", "mention", "New", "2023-10-02T10:00:00Z")
        repository.insertNotifications(listOf(n1, n2))

        val items = collectPagingData()
        // Header + Newest + Oldest
        assertEquals(2L, items[1].notification?.id)
        assertEquals(1L, items[2].notification?.id)
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
        val itemsHeaderFiltered = collectPagingData()
        assertEquals(2, itemsHeaderFiltered.size) // Header + 1 match
        assertEquals(1L, itemsHeaderFiltered[1].notification?.id)

        // check if match on body is reported
        viewModel.updateSearchQuery("ora")
        val itemsBodyFiltered = collectPagingData()
        assertEquals(2, itemsBodyFiltered.size)
        assertEquals(1L, itemsBodyFiltered[1].notification?.id)

        // check if match on title is reported
        viewModel.updateSearchQuery("kiw")
        val itemsTitleFiltered = collectPagingData()
        assertEquals(2, itemsTitleFiltered.size)
        assertEquals(1L, itemsTitleFiltered[1].notification?.id)

        // check if match on links is reported
        viewModel.updateSearchQuery("mel")
        val itemsLinkFiltered = collectPagingData()
        assertEquals(2, itemsLinkFiltered.size)
        assertEquals(1L, itemsLinkFiltered[1].notification?.id)
    }

    @Test
    fun testHideReadFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Unread")
        val n2 = createNotification(2, "en", "mention", "Read")
        n2.read = "2023-10-01T10:00:00Z"
        repository.insertNotifications(listOf(n1, n2))

        preferences.hideRead = true
        // Force refresh to apply preference change
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData()
        assertEquals(2, itemsFiltered.size) // Header + 1 unread
        assertEquals(1L, itemsFiltered[1].notification?.id)

        preferences.hideRead = false
        viewModel.fetchAndSave(true)

        val itemsUnfiltered = collectPagingData()
        assertEquals(3, itemsUnfiltered.size) // Header + 2 items
    }

    @Test
    fun testWikiFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "zh", "mention", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedWikis.add("en")
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData()
        assertEquals(2, itemsFiltered.size) // Header + 1 match (zh)
        assertEquals(2L, itemsFiltered[1].notification?.id)
    }

    @Test
    fun testTypeFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "en", "edit-thank", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedTypes.add("edit-thank")
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData()
        assertEquals(2, itemsFiltered.size) // Header + 1 match
        assertEquals(1L, itemsFiltered[1].notification?.id)
    }

    @Test
    fun testTabFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Mention")
        val n2 = createNotification(2, "en", "edit-thank", "Not Mention")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.updateTabSelection(1) // Mentions
        val itemsFiltered = collectPagingData()
        assertEquals(2, itemsFiltered.size)
        assertEquals(1L, itemsFiltered[1].notification?.id)

        viewModel.updateTabSelection(0) // All
        val itemsUnfiltered = collectPagingData()
        assertEquals(3, itemsUnfiltered.size)
    }

    private fun createNotification(
        id: Long,
        wiki: String,
        category: String,
        header: String,
        timestamp: String = "2022-10-27T14:45:00.123Z",
        read: String? = null,
        title: String = "",
        body: String = "",
        links: String = "",
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
                            { "label": "$links", "url": "" }
                        ]
                    }
                }
            }
        """.trimIndent()

        return JsonUtil.decodeFromString<Notification>(json)!!
    }

    private class NotificationItemAdapter : PagingDataAdapter<NotificationListItemContainer, androidx.recyclerview.widget.RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<NotificationListItemContainer>() {
            override fun areItemsTheSame(oldItem: NotificationListItemContainer, newItem: NotificationListItemContainer) = oldItem === newItem
            override fun areContentsTheSame(oldItem: NotificationListItemContainer, newItem: NotificationListItemContainer) = oldItem == newItem
        }
    ) {
        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {}
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder = mockk(relaxed = true)
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
        private val remoteKeys = mutableMapOf<String, String?>()

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

        override suspend fun markItemsAsRead(ids: List<Long>, readTimestamp: String?) {
            notificationDao.markItemsAsRead(ids, readTimestamp)
        }

        override fun getNotificationsFlow(
            hideReadNotifications: Boolean,
            searchQuery: String?,
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>,
            hideNotMentioned: Boolean
        ): Flow<PagingData<Notification>> {
            return Pager(
                config = PagingConfig(pageSize = 50),
                pagingSourceFactory = {
                    notificationDao.getAllSelectedNotificationPaged(
                        hideReadNotifications,
                        searchQuery,
                        !excludedTypeCodes.isEmpty(),
                        excludedTypeCodes,
                        includedWikiCodes,
                        hideNotMentioned,
                        NotificationCategory.MENTIONS_GROUP.map { it.id }
                    )
                }
            ).flow
        }

        override fun getUnreadCountsFlow(
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>
        ): Flow<Pair<Int, Int>> {
            return combine(
                notificationDao.getUnreadCount(excludedTypeCodes, includedWikiCodes),
                notificationDao.getUnreadMentionsCount(
                    excludedTypeCodes,
                    includedWikiCodes,
                    NotificationCategory.MENTIONS_GROUP.map { it.id })
            ) { all, mentions -> all to mentions }
        }

        override suspend fun getRemoteKey(wiki: String): String? = remoteKeys[wiki]
        override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) {
            remoteKeys[wiki] = nextContinueStr 
        }
        override suspend fun clearRemoteKeys() { remoteKeys.clear() }

        override suspend fun getEndOfPaginationReachedFlow(): Flow<Boolean> {
            return flowOf(false) // dummy not used in this test class
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
