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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        assertTrue(attempts < maxAttempts) // ensure test case fails if database op is not finished
    }

    /**
     * Helper function to capture a snapshot of the Paging 3 [PagingData] stream for assertions.
     *
     * Paging 3 is inherently asynchronous and designed for UI consumption. In a unit test,
     * we cannot simply read the value of a [Flow]<[PagingData]>. Instead, this function:
     * 1. Creates a temporary [NotificationItemAdapter] (the standard way to "consume" Paging data).
     * 2. Launches a background coroutine to collect the flow into that adapter.
     * 3. Idles the Robolectric [Shadows] looper and uses [kotlinx.coroutines.delay] to allow the
     *    background Room query and Paging mapping operations to complete.
     * 4. Returns a static [List] of the items currently held by the adapter snapshot.
     */
    private suspend fun collectPagingData(checkAttempts: Boolean): List<NotificationListItemContainer> = coroutineScope {
        val adapter = NotificationItemAdapter()
        val job = launch {
            viewModel.notificationFlow.collectLatest {
                adapter.submitData(it)
            }
        }

        // Wait for Paging to finish initial load by idling the looper until data is available
        var attempts = 0
        val maxAttempts = 200
        while (adapter.itemCount == 0 && attempts < maxAttempts) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            kotlinx.coroutines.delay(20)
            attempts++
        }
        if (checkAttempts) {
            assertTrue(attempts < maxAttempts) // ensure test case fails if flow is not finished
        }
        val items = adapter.snapshot().items
        job.cancel()
        items
    }

    /**
     * Verifies basic notification loading and posting to the UI.
     *
     * This test ensures that notifications inserted into the database are correctly emitted
     * through the [viewModel.notificationFlow]. It validates that the reactive pipeline
     * correctly bundles database items with UI headers (like the search bar).
     */
    @Test
    fun testFilterAndPostNotifications() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "H1")
        val n2 = createNotification(2, "zh", "edit-thank", "H2")
        repository.insertNotifications(listOf(n1, n2))

        val items = collectPagingData(true)
        // 2 items + 1 search bar header
        assertEquals(3, items.size)
        assertEquals(1L, items[1].notification?.id)
    }

    /**
     * Verifies that notifications are sorted correctly by timestamp in descending order.
     *
     * In the legacy [NotificationViewModel], sorting is performed in memory using
     * Kotlin collection transformations. This test ensures that the Room-based 
     * [org.wikipedia.notifications.db.NotificationDao] query correctly maintains this 
     * requirement by applying 'ORDER BY timestamp DESC' at the database level.
     */
    @Test
    fun testSorting() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Old", "2023-10-01T10:00:00Z")
        val n2 = createNotification(2, "en", "mention", "New", "2023-10-02T10:00:00Z")
        repository.insertNotifications(listOf(n1, n2))

        val items = collectPagingData(true)
        // Header + Newest + Oldest
        assertEquals(2L, items[1].notification?.id)
        assertEquals(1L, items[2].notification?.id)
    }

    /**
     * Verifies that the search query correctly filters notifications.
     *
     * This test replicates the search logic from the legacy [NotificationViewModel.processList],
     * which performs a case-insensitive match on the notification's header, body, title,
     * and secondary link labels. In the refactored version, this filtering is performed
     * by the database using SQL 'LIKE' operators.
     */
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
        val itemsHeaderFiltered = collectPagingData(true)
        assertEquals(2, itemsHeaderFiltered.size) // Header + 1 match
        assertEquals(1L, itemsHeaderFiltered[1].notification?.id)

        // check if match on body is reported
        viewModel.updateSearchQuery("ora")
        val itemsBodyFiltered = collectPagingData(true)
        assertEquals(2, itemsBodyFiltered.size)
        assertEquals(1L, itemsBodyFiltered[1].notification?.id)

        // check if match on title is reported
        viewModel.updateSearchQuery("kiw")
        val itemsTitleFiltered = collectPagingData(true)
        assertEquals(2, itemsTitleFiltered.size)
        assertEquals(1L, itemsTitleFiltered[1].notification?.id)

        // check if match on links is reported
        viewModel.updateSearchQuery("mel")
        val itemsLinkFiltered = collectPagingData(true)
        assertEquals(2, itemsLinkFiltered.size)
        assertEquals(1L, itemsLinkFiltered[1].notification?.id)
    }

    /**
     * Verifies that read notifications are filtered based on user preferences.
     *
     * This test replicates the logic from legacy [NotificationViewModel.processList] which
     * conditionally skips notifications if [NotificationPreferences.isHideReadNotificationsEnabled]
     * is true and the notification is not unread. In the refactored version, this is 
     * handled by the DAO query using 'read IS NULL' logic.
     */
    @Test
    fun testHideReadFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Unread")
        val n2 = createNotification(2, "en", "mention", "Read")
        n2.read = "2023-10-01T10:00:00Z"
        repository.insertNotifications(listOf(n1, n2))

        preferences.hideRead = true
        // Force refresh to apply preference change
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData(true)
        assertEquals(2, itemsFiltered.size) // Header + 1 unread
        assertEquals(1L, itemsFiltered[1].notification?.id)

        preferences.hideRead = false
        viewModel.fetchAndSave(true)

        val itemsUnfiltered = collectPagingData(true)
        assertEquals(3, itemsUnfiltered.size) // Header + 2 items
    }

    /**
     * Verifies that notifications are filtered correctly based on their source wiki.
     *
     * This test replicates the logic from [NotificationViewModel.processList] which
     * filters out notifications from wikis specified in
     * [NotificationPreferences.getNotificationExcludedWikiCodes].
     * In the refactored version, this filtering is performed by the database using SQL
     * string manipulation to extract the language code from the wiki database name.
     */
    @Test
    fun testWikiFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "zh", "mention", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedWikis.add("en")
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData(true)
        assertEquals(2, itemsFiltered.size) // Header + 1 match (zh)
        assertEquals(2L, itemsFiltered[1].notification?.id)
    }

    /**
     * Verifies that category exclusions correctly handle subtypes using prefix matching.
     *
     * This test replicates the logic from [NotificationViewModel.processList] which
     * uses 'startsWith' to exclude all sub-categories belonging to a base type (e.g.,
     * excluding 'thank-you-edit' should also hide 'thank-you-edit-milestone').
     * In the refactored version, this is handled by the DAO query using SQL 'LIKE' patterns.
     */
    @Test
    fun testTypeFilteringPrefix() = runBlocking {
        val n1 = createNotification(1, "en", "thank-you-edit", "Basic")
        val n2 = createNotification(2, "en", "thank-you-edit-milestone", "Milestone")
        val n3 = createNotification(3, "en", "mention", "Included")
        repository.insertNotifications(listOf(n1, n2, n3))

        preferences.excludedTypes.add("thank-you-edit")
        viewModel.fetchAndSave(true)

        val itemsFiltered = collectPagingData(true)
        assertEquals(2, itemsFiltered.size) // Header + 1 match (mention)
        assertEquals(3L, itemsFiltered[1].notification?.id)
    }

    /**
     * Verifies that the "Mentions" tab correctly includes sub-categories using prefix matching.
     *
     * This test replicates the logic from legacy [NotificationViewModel.processList] and
     * [NotificationCategory.isMentionsGroup], which use 'startsWith' to include all 
     * sub-categories belonging to a mention group (e.g., 'edit-user-talk-v2'). 
     * In the refactored version, this is handled by the DAO query using SQL 'LIKE' patterns.
     */
    @Test
    fun testMentionsPrefixFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "edit-user-talk-v2", "Talk sub-type")
        val n2 = createNotification(2, "en", "system-generic", "Not a mention")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.updateTabSelection(1) // Mentions
        val itemsFiltered = collectPagingData(true)
        assertEquals(2, itemsFiltered.size) // Header + 1 match
        assertEquals(1L, itemsFiltered[1].notification?.id)
    }

    /**
     * Verifies that the search bar header is dynamically removed during multi-select mode.
     *
     * In the legacy implementation, [NotificationActivity] manually managed the search bar
     * visibility during action mode. This test ensures that the refactored ViewModel 
     * reactively removes the header item from the [viewModel.notificationFlow] when 
     * [viewModel.isSearchVisible] is set to false, matching the required UI behavior.
     */
    @Test
    fun testSearchVisibilityInMultiSelect() = runBlocking {
        assertTrue(viewModel.isSearchVisible)
        
        viewModel.isSearchVisible = false // Simulated setting from Activity when ActionMode starts
        val itemsHidden = collectPagingData(false)
        assertEquals(0, itemsHidden.filter { it.notification == null }.size)

        viewModel.isSearchVisible = true
        val itemsVisible = collectPagingData(false)
        assertEquals(1, itemsVisible.filter { it.notification == null }.size)
    }

    /**
     * Verifies that unread counts are updated reactively when the database changes.
     *
     * In the legacy [NotificationViewModel], unread counts were recalculated 
     * manually as part of the [NotificationViewModel.processList] method. 
     * This test ensures that the refactored ViewModel automatically updates its 
     * [viewModel.allUnreadCount] and [viewModel.mentionsUnreadCount] properties 
     * whenever the underlying database state changes (e.g., after marking an item 
     * as read), using reactive repository flows.
     */
    @Test
    fun testUnreadCountsReactivity() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Unread")
        val n2 = createNotification(2, "en", "mention", "Read")
        n2.read = "2023-10-01T10:00:00Z"
        repository.insertNotifications(listOf(n1, n2))

        waitForViewModel()
        assertEquals(1, viewModel.allUnreadCount)
        assertEquals(1, viewModel.mentionsUnreadCount)

        // Mark n1 as read
        repository.markItemsAsRead(listOf(1L), "2023-10-02T10:00:00Z")
        
        // Wait for reactive update
        waitForViewModel()
        assertEquals(0, viewModel.allUnreadCount)
    }

    // helper function to create mock data for test cases
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

    // Dummy adapter used to consume Paging 3 data and capture snapshots for assertions
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
                    notificationDao.getAllSelectedNotificationPagedFullLegacy(
                        hideReadNotifications,
                        searchQuery,
                        includedWikiCodes,
                        hideNotMentioned,
                        excludedTypeCodes.contains(NotificationCategory.SYSTEM.id),
                        excludedTypeCodes.contains(NotificationCategory.MILESTONE_EDIT.id),
                        excludedTypeCodes.contains(NotificationCategory.EDIT_USER_TALK.id),
                        excludedTypeCodes.contains(NotificationCategory.EDIT_THANK.id),
                        excludedTypeCodes.contains(NotificationCategory.REVERTED.id),
                        excludedTypeCodes.contains(NotificationCategory.LOGIN_FAIL.id),
                        excludedTypeCodes.contains(NotificationCategory.MENTION.id),
                        excludedTypeCodes.contains(NotificationCategory.EMAIL_USER.id),
                        excludedTypeCodes.contains(NotificationCategory.USER_RIGHTS.id),
                        excludedTypeCodes.contains(NotificationCategory.ARTICLE_LINKED.id),
                        excludedTypeCodes.contains(NotificationCategory.ALPHA_BUILD_CHECKER.id),
                        excludedTypeCodes.contains(NotificationCategory.READING_LIST_SYNCING.id),
                        excludedTypeCodes.contains(NotificationCategory.SYNCING.id),
                        excludedTypeCodes.contains(NotificationCategory.RECOMMENDED_READING_LISTS.id),
                        excludedTypeCodes.contains(NotificationCategory.GAMES.id)
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
            return flowOf(true)
        }
    }

    // Fake filter helper used for mocking during test informing all
    // - wikis (language codes)
    // - types/categories
    private class FakeNotificationFilterHelper: NotificationFilterHelper {
        override fun allWikisList(): List<String> {
            return listOf("en", "zh", "dummy")
        }

        override fun allTypesIdList(): List<String> {
            return listOf("type1", "type2")
        }
    }
}
