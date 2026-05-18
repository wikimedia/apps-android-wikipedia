package org.wikipedia.notifications

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.util.Resource
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class NotificationRefactoredViewModelPerformanceTest {
    val numberNotifications = 1000
    val numberIterations = 100

    private lateinit var viewModel: NotificationRefactoredViewModel
    private lateinit var notificationRepository: FakeNotificationRepository
    private val notificationFilterHelper = FakeNotificationFilterHelper()
    private val notificationPreferences = FakeNotificationPreferences()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        runBlocking {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            db = androidx.room.Room.databaseBuilder(
                context, AppDatabase::class.java, org.wikipedia.database.DATABASE_NAME
            )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(false)
                .build()
            db.notificationDao().deleteAll()
            notificationRepository = FakeNotificationRepository(db.notificationDao())

            viewModel = NotificationRefactoredViewModel(
                notificationPreferences,
                notificationRepository,
                notificationFilterHelper
            )

            // Wait for view model to initialize - without blocking the thread
            withTimeout(5000) { // 5 second timeout
                viewModel.uiState.filter { it is Resource.Success }.first()
            }
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            db.notificationDao().deleteAll()
            db.notificationRemoteKeyDao().deleteAll()
            db.close()
        }
    }

    @Test
    fun testPerformance() = runBlocking {
        // Generate notifications
        val notifications = NotificationPerformanceTestStimuliGenerator.generateNotifications(numberNotifications)
        println("Adding ${notifications.size} notifications to mock repository...")
        notificationRepository.insertNotifications(notifications)

        // Apply a combination of filters
        notificationPreferences.hideRead = true
        notificationPreferences.excludedWikis.add("en")
        notificationPreferences.excludedTypes.add("type1")
        viewModel.updateSearchQuery("Header 1") // Will limit to headers starting with "Header 1"

        // Perform an initial fetch to ensure uiState is populated with Success data
        // which simplifies the drop(1) logic in the measurement loop.
        viewModel.fetchAndSave(refresh = true)
        
        // Use the PagingDataAdapter to observe the flow during the test
        val adapter = NotificationItemAdapter()
        val job = launch {
            viewModel.notificationFlow.collectLatest {
                adapter.submitData(it)
            }
        }

        // Wait for initialization and initial fetch to finish without blocking
        var initialAttempts = 0
        while (adapter.itemCount == 0 && initialAttempts < 100) {
            delay(10)
            initialAttempts++
        }

        val times = mutableListOf<Long>()

        println("Starting performance measurement ($numberNotifications notifications, $numberIterations iterations)...")

        repeat(numberIterations) { iteration ->
            val time = measureTimeMillis {
                viewModel.fetchAndSave(refresh = true)
                // Use the adapter's loadStateFlow to wait for the result
                adapter.loadStateFlow
                    .filter { it.refresh is LoadState.NotLoading }
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
        val medianTime = (sortedTimes[numberIterations / 2 - 1] + sortedTimes[numberIterations / 2]) / 2.0

        println("\nPerformance Result Summary ($numberNotifications notifications, $numberIterations iterations):")
        println("Min: $minTime ms")
        println("Max: $maxTime ms")
        println("Average: ${"%.2f".format(averageTime)} ms")
        println("Median: ${"%.2f".format(medianTime)} ms")

        // Log count of items for verification
        println("Final item count in UI snapshot: ${adapter.itemCount}")
        job.cancel()
    }

    private class NotificationItemAdapter : PagingDataAdapter<NotificationListItemContainer, androidx.recyclerview.widget.RecyclerView.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<NotificationListItemContainer>() {
            override fun areItemsTheSame(oldItem: NotificationListItemContainer, newItem: NotificationListItemContainer) = oldItem === newItem
            override fun areContentsTheSame(oldItem: NotificationListItemContainer, newItem: NotificationListItemContainer) = oldItem == newItem
        }
    ) {
        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {}
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(android.view.View(parent.context)) {}
        }
    }

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

        override suspend fun markItemsAsRead(
            ids: List<Long>,
            readTimestamp: String?
        ) {
            notificationDao.markItemsAsRead(ids, readTimestamp)
        }

        override fun getNotificationsFlow(
            hideReadNotifications: Boolean,
            searchQuery: String?,
            excludedTypeCodes: Set<String>,
            includedWikiCodes: List<String>,
            hideNotMentioned: Boolean
        ): Flow<PagingData<Notification>> {
            return androidx.paging.Pager(
                config = androidx.paging.PagingConfig(pageSize = 50),
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
        override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) { remoteKeys[wiki] = nextContinueStr }
        override suspend fun getEndOfPaginationReachedFlow(): Flow<Boolean> {
            return flowOf(false) // not used in this test suite
        }

        override suspend fun clearRemoteKeys() { remoteKeys.clear() }
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
