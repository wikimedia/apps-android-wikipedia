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
import org.wikipedia.util.Resource

/**
 *  Test class for testing the functionality of a notification view model that allows implementation
 *  of the Paging3 library. The test cases are unit-tests executed on the local developer machine.
 *  The test class mainly focuses on the filtering and sorting functionality.
 *  For reference purposes, the test class can be switched between using the legacy-code or the
 *  refactored code. Pending review feedback, the legacy testing may be removed in future revisions.
 *
 *  The class is not meant to test the performance of the view model. See the related instrumented
 *  tests in androidTest folder.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationViewModelTest {
    private val legacy = false
    private lateinit var repository: NotificationRepository
    private val preferences = FakeNotificationPreferences()
    private val notificationHelper = FakeNotificationFilterHelper()
    private val wikipediaApp = mockk<WikipediaApp>(relaxed = true)
    private lateinit var viewModel: NotificationViewModel
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        mockkObject(WikipediaApp)
        every { WikipediaApp.instance } returns wikipediaApp
        every { wikipediaApp.isOnline } returns true

        repository = if (legacy) {
            FakeNotificationLegacyRepository()
        } else {
            val context = org.robolectric.RuntimeEnvironment.getApplication()
            db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            FakeNotificationRefactoredRepository(db.notificationDao())
        }

        mockkObject(NotificationFilterActivity)
        every { NotificationFilterActivity.allWikisList() } returns listOf("en", "zh")
        every { NotificationFilterActivity.allTypesIdList() } returns listOf("edit-thank", "mention")

        viewModel = if (legacy) {
            NotificationLegacyViewModelImpl(
                preferences,
                repository,
                notificationHelper
            )
        } else {
            NotificationRefactoredViewModelImpl(
                preferences,
                repository,
                notificationHelper
            )
        }

        // Wait for view model to initialize
        if (!legacy) {
            waitForViewModel()
        }
    }

    @After
    fun tearDown() = runBlocking {
        if (!legacy) {
            withContext(Dispatchers.IO) {
                db.close()
            }
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
            val refactoredViewModel = (viewModel as NotificationRefactoredViewModelImpl)
            refactoredViewModel.notificationFlow.collectLatest {
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
     * through the view model's notification flow. It validates that the reactive pipeline
     * correctly bundles database items with UI headers (like the search bar).
     */
    @Test
    fun testFilterAndPostNotifications() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "H1")
        val n2 = createNotification(2, "zh", "edit-thank", "H2")
        repository.insertNotifications(listOf(n1, n2))

        if (legacy) {
            viewModel.fetchAndSave()
            val uiState = viewModel.uiState.value
            assertTrue(uiState is Resource.Success)
            assertEquals(2, (uiState as Resource.Success).data.first.size)
        } else {
            val items = collectPagingData(true)
            // 2 items + 1 search bar header
            assertEquals(3, items.size)
            assertEquals(1L, items[1].notification?.id)
        }
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
        val n1 = createNotification(
            1,
            "en",
            "mention",
            "Old",
            "2023-10-01T10:00:00Z"
        )
        val n2 = createNotification(
            2,
            "en",
            "mention",
            "New",
            "2023-10-02T10:00:00Z"
        )
        repository.insertNotifications(listOf(n1, n2))

        if (legacy) {
            viewModel.fetchAndSave()
            val uiState = viewModel.uiState.value
            val items = (uiState as Resource.Success).data.first
            assertEquals(2L, items[0].notification?.id)
            assertEquals(1L, items[1].notification?.id)
        } else {
            val items = collectPagingData(true)
            // Header + Newest + Oldest
            assertEquals(2L, items[1].notification?.id)
            assertEquals(1L, items[2].notification?.id)
        }
    }

    /**
     * Verifies that the search query correctly filters notifications.
     *
     * This test focuses on the search functionality as established in the legacy implementation,
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
        if (legacy) {
            val uiStateHeaderFiltered = viewModel.uiState.value
            val itemsHeaderFiltered = (uiStateHeaderFiltered as Resource.Success).data.first
            assertEquals(1, itemsHeaderFiltered.size)
            assertEquals(1L, itemsHeaderFiltered[0].notification?.id)
        } else {
            val itemsHeaderFiltered = collectPagingData(true)
            assertEquals(2, itemsHeaderFiltered.size) // Header + 1 match
            assertEquals(1L, itemsHeaderFiltered[1].notification?.id)
        }

        // check if match on body is reported
        viewModel.updateSearchQuery("ora")
        if (legacy) {
            val uiStateBodyFiltered = viewModel.uiState.value
            val itemsBodyFiltered = (uiStateBodyFiltered as Resource.Success).data.first
            assertEquals(1, itemsBodyFiltered.size)
            assertEquals(1L, itemsBodyFiltered[0].notification?.id)
        } else {
            val itemsBodyFiltered = collectPagingData(true)
            assertEquals(2, itemsBodyFiltered.size)
            assertEquals(1L, itemsBodyFiltered[1].notification?.id)
        }

        // check if match on title is reported
        viewModel.updateSearchQuery("kiw")
        if (legacy) {
            val uiStateTitleFiltered = viewModel.uiState.value
            val itemsTitleFiltered = (uiStateTitleFiltered as Resource.Success).data.first
            assertEquals(1, itemsTitleFiltered.size)
            assertEquals(1L, itemsTitleFiltered[0].notification?.id)

        } else {
            val itemsTitleFiltered = collectPagingData(true)
            assertEquals(2, itemsTitleFiltered.size)
            assertEquals(1L, itemsTitleFiltered[1].notification?.id)
        }

        // check if match on links is reported
        viewModel.updateSearchQuery("mel")
        if (legacy) {
            val uiStateLinkFiltered = viewModel.uiState.value
            val itemsLinkFiltered = (uiStateLinkFiltered as Resource.Success).data.first
            assertEquals(1, itemsLinkFiltered.size)
            assertEquals(1L, itemsLinkFiltered[0].notification?.id)
        } else {
            val itemsLinkFiltered = collectPagingData(true)
            assertEquals(2, itemsLinkFiltered.size)
            assertEquals(1L, itemsLinkFiltered[1].notification?.id)
        }
    }

    /**
     * Verifies that read notifications are filtered based on user preferences.
     *
     * This test checks the filtering logic for notifications which have been read by the user:
     * Skipping notifications if [NotificationPreferences.isHideReadNotificationsEnabled]
     * is true and the notification is not unread. In the refactored version, this is 
     * handled by the DAO query using 'read IS NULL' logic.
     */
    @Test
    fun testHideReadFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Unread")
        val n2 = createNotification(2, "en", "mention", "Read")
        n2.read = "2023-10-01T10:00:00Z"
        repository.insertNotifications(listOf(n1, n2))

        preferences.hideRead = true // set the fake preference to filter out read notifications
        if (legacy) {
            viewModel.fetchAndSave() // triggers reading, storage and update of uiState
            val uiStateFiltered = viewModel.uiState.value
            val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
            assertEquals(1, itemsFiltered.size)
            assertEquals(1L, itemsFiltered[0].notification?.id)
        } else {
            viewModel.fetchAndSave(true) // Force refresh to apply preference change
            val itemsFiltered = collectPagingData(true)
            assertEquals(2, itemsFiltered.size) // Header + 1 unread
            assertEquals(1L, itemsFiltered[1].notification?.id)
        }

        preferences.hideRead = false // set the fake preference to not filter out read notifications
        if (legacy) {
            viewModel.fetchAndSave() // triggers API reading, storage and update of uiState
            val uiStateUnfiltered = viewModel.uiState.value
            val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
            assertEquals(2, itemsUnfiltered.size)
            assertEquals(1L, itemsUnfiltered[0].notification?.id)
            assertEquals(2L, itemsUnfiltered[1].notification?.id)
        } else {
            viewModel.fetchAndSave(true)
            val itemsUnfiltered = collectPagingData(true)
            assertEquals(3, itemsUnfiltered.size) // Header + 2 items
        }
    }

    /**
     * Verifies that notifications are filtered correctly based on their source wiki.
     *
     * This test checks the logic for filtering notifications from certain sources. It filters out
     * notifications from wikis specified in [NotificationPreferences.getNotificationExcludedWikiCodes].
     * In the refactored version, this filtering is performed by the database using SQL
     * string manipulation to extract the language code from the wiki database name.
     */
    @Test
    fun testWikiFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "mention", "Included")
        val n2 = createNotification(2, "zh", "mention", "Excluded")
        repository.insertNotifications(listOf(n1, n2))

        preferences.excludedWikis.add("en") // add "en" to the list of wikis to be filtered out
        if (legacy) {
            viewModel.fetchAndSave() // triggers reading, storage and update of uiState
            val uiStateFiltered = viewModel.uiState.value
            val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
            assertEquals(1, itemsFiltered.size)
            assertEquals(2L, itemsFiltered[0].notification?.id)
        } else {
            viewModel.fetchAndSave(true)
            val itemsFiltered = collectPagingData(true)
            assertEquals(2, itemsFiltered.size) // Header + 1 match (zh)
            assertEquals(2L, itemsFiltered[1].notification?.id)
        }

        preferences.excludedWikis.remove("en") // remove "en" from the list
        if (legacy) {
            viewModel.fetchAndSave() // triggers reading, storage and update of uiState
            val uiStateUnfiltered = viewModel.uiState.value
            val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
            assertEquals(2, itemsUnfiltered.size)
            assertEquals(1L, itemsUnfiltered[0].notification?.id)
            assertEquals(2L, itemsUnfiltered[1].notification?.id)
        } else {
            viewModel.fetchAndSave(true)
            val itemsUnfiltered = collectPagingData(true)
            assertEquals(3, itemsUnfiltered.size) // Header + 1 match (zh)
            assertEquals(1L, itemsUnfiltered[1].notification?.id)
            assertEquals(2L, itemsUnfiltered[2].notification?.id)
        }

    }

    /**
     * Verifies that category exclusions correctly handle subtypes using prefix matching.
     *
     * This checks the logic for filtering certain categories using 'startsWith' to exclude
     * all sub-categories belonging to a base type (e.g., excluding 'thank-you-edit' should also
     * hide 'thank-you-edit-milestone').
     * In the refactored version, this is handled by the DAO query using SQL 'LIKE' patterns.
     */
    @Test
    fun testTypeFilteringPrefix() = runBlocking {
        val n1 = createNotification(1, "en", "thank-you-edit", "Basic")
        val n2 = createNotification(2, "en", "thank-you-edit-milestone", "Milestone")
        val n3 = createNotification(3, "en", "mention", "Included")
        repository.insertNotifications(listOf(n1, n2, n3))

        preferences.excludedTypes.add("thank-you-edit")
        if (legacy) {
            viewModel.fetchAndSave() // triggers reading, storage and update of uiState
            val uiStateFiltered = viewModel.uiState.value
            val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
            assertEquals(1, itemsFiltered.size)
            assertEquals(3L, itemsFiltered[0].notification?.id)
        } else {
            viewModel.fetchAndSave(true)
            val itemsFiltered = collectPagingData(true)
            assertEquals(2, itemsFiltered.size) // Header + 1 match (mention)
            assertEquals(3L, itemsFiltered[1].notification?.id)
        }
    }

    /**
     * Verifies that the "Mentions" tab correctly includes sub-categories using prefix matching.
     *
     * This checks the logic for switching between "All" and "Mentions" tab.
     * In the refactored version, this is handled by the DAO query using SQL 'LIKE' patterns.
     */
    @Test
    fun testMentionsPrefixFiltering() = runBlocking {
        val n1 = createNotification(1, "en", "edit-user-talk-v2", "Talk sub-type")
        val n2 = createNotification(2, "en", "system-generic", "Not a mention")
        repository.insertNotifications(listOf(n1, n2))

        viewModel.updateTabSelection(1) // Mentions
        if (legacy) {
            val uiStateFiltered = viewModel.uiState.value
            val itemsFiltered = (uiStateFiltered as Resource.Success).data.first
            assertEquals(1, itemsFiltered.size) // 1 match
            assertEquals(1L, itemsFiltered[0].notification?.id)
        } else {
            val itemsFiltered = collectPagingData(true)
            assertEquals(2, itemsFiltered.size) // Header + 1 match
            assertEquals(1L, itemsFiltered[1].notification?.id)
        }
        viewModel.updateTabSelection(0) // All
        if (legacy) {
            val uiStateUnfiltered = viewModel.uiState.value
            val itemsUnfiltered = (uiStateUnfiltered as Resource.Success).data.first
            assertEquals(2, itemsUnfiltered.size) // 1 match
            assertEquals(1L, itemsUnfiltered[0].notification?.id)
            assertEquals(2L, itemsUnfiltered[1].notification?.id)
        } else {
            val itemsUnfiltered = collectPagingData(true)
            assertEquals(3, itemsUnfiltered.size) // Header + 1 match
            assertEquals(1L, itemsUnfiltered[1].notification?.id)
            assertEquals(2L, itemsUnfiltered[2].notification?.id)
        }
    }

    /**
     * Verifies that the search bar header is dynamically removed during multi-select mode.
     *
     * In the legacy implementation, [NotificationActivity] manually managed the search bar
     * visibility during action mode. This test ensures that the refactored view model
     * reactively removes the header item from the notification flow when
     * isSearchVisible is set to false, matching the required UI behavior.
     */
    @Test
    fun testSearchVisibilityInMultiSelect() = runBlocking {
        if (!legacy && viewModel is NotificationRefactoredViewModelImpl) {
            val refactoredViewModel = viewModel as NotificationRefactoredViewModelImpl
            assertTrue(refactoredViewModel.isSearchVisible)

            refactoredViewModel.isSearchVisible =
                false // Simulated setting from Activity when ActionMode starts
            val itemsHidden = collectPagingData(false)
            assertEquals(0, itemsHidden.filter { it.notification == null }.size)

            refactoredViewModel.isSearchVisible = true
            val itemsVisible = collectPagingData(false)
            assertEquals(1, itemsVisible.filter { it.notification == null }.size)
        }
    }

    /**
     * Verifies that unread counts are updated reactively when the database changes.
     *
     * In the legacy notification view model, unread counts were recalculated in Kotlin code.
     * as part of the processList method.
     * This test ensures that the refactored view model automatically updates its
     * allUnreadCount and mentionsUnreadCount properties whenever the underlying database state
     * changes (e.g., after marking an item as read), using reactive repository flows.
     */
    @Test
    fun testUnreadCountsReactivity() = runBlocking {
        if (!legacy) {
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
    private class FakeNotificationRefactoredRepository(
        private val notificationDao: NotificationDao
    ) : NotificationRepository {
        var unreadWikis = mapOf<String, WikiSite>()
        private val remoteKeys = mutableMapOf<String, String?>()

        override suspend fun insertNotifications(notificationList: List<Notification>) {
            notificationDao.insertNotifications(notificationList)
        }
        override suspend fun getAllNotifications(): List<Notification> {
            return notificationDao.getAllNotifications()
        }
        override suspend fun updateNotification(notification: Notification) {
            notificationDao.updateNotification(notification)
        }
        override suspend fun fetchUnreadWikiDbNames() = unreadWikis
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = null

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
                    notificationDao.getAllSelectedNotificationsPaged(
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
                notificationDao.getUnreadCount(
                    includedWikiCodes,
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
                ),
                notificationDao.getUnreadMentionsCount(
                    includedWikiCodes,
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

    // Fake notification repository used for mocking during test of legacy code
    private class FakeNotificationLegacyRepository : NotificationRepository {
        val notifications = mutableListOf<Notification>()
        var unreadWikis = mapOf<String, WikiSite>()
        private val remoteKeys = mutableMapOf<String, String?>()

        override suspend fun insertNotifications(notificationList: List<Notification>) {
            this.notifications.addAll(notificationList)
        }
        override suspend fun getAllNotifications() = notifications
        override suspend fun updateNotification(notification: Notification) {
            val index = notifications.indexOfFirst { it.id == notification.id && it.wiki == notification.wiki }
            if (index != -1) {
                notifications[index] = notification
            }
        }
        override suspend fun fetchUnreadWikiDbNames() = unreadWikis
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = null

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
            return flowOf(PagingData.from(notifications))
        }

        override fun getUnreadCountsFlow(
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>
        ): Flow<Pair<Int, Int>> {
            return flowOf(0 to 0)
        }

        override suspend fun getRemoteKey(wiki: String): String? = remoteKeys[wiki]
        override suspend fun clearRemoteKeys() { remoteKeys.clear() }
        override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) {
            // not used in this test class
        }

        override suspend fun getEndOfPaginationReachedFlow(): Flow<Boolean> {
            return flowOf(false) // not used in this test class
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
