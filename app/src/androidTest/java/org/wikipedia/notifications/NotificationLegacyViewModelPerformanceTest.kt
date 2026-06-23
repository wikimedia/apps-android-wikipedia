package org.wikipedia.notifications

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.Resource
import kotlin.system.measureTimeMillis

/**
 *  Test class for testing the performance of the legacy notification view model that does not allow
 *  implementation of the Paging3 library.
 *  The test cases are instrumented test cases which should be executed on a physical device.
 *  The test case measures the execution time and reports it in the terminal.
 *
 *  Pending review feedback, this test class may be removed in future revisions.
 *
 *  The class is not meant to test the functionality of the view model. See the related unit
 *  tests in test folder.
 */
@RunWith(AndroidJUnit4::class)
class NotificationLegacyViewModelPerformanceTest {
    private val numberNotifications = 1000
    private val numberIterations = 100
    private val notificationRepository = FakeNotificationRepository()
    private val notificationPreferences = FakeNotificationPreferences()
    private val notificationFilterHelper = FakeNotificationFilterHelper()
    private lateinit var viewModel: NotificationLegacyViewModelImpl

    @Before
    fun setUp() {
        runBlocking {
            viewModel = NotificationLegacyViewModelImpl(
                notificationPreferences,
                notificationRepository,
                notificationFilterHelper
            )

            // Wait for view model to initialize without blocking the instrumentation thread
            withTimeout(100000) {
                viewModel.uiState.filter { it is Resource.Success }.first()
            }
        }
    }

    @Test
    fun testPerformance() = runBlocking {
        // Generate notifications
        val notifications = NotificationPerformanceTestStimuliGenerator.generateNotifications(numberNotifications)
        println("Adding ${notifications.size} notifications to mock repository...")
        notificationRepository.notifications.addAll(notifications)

        // Apply a combination of filters
        notificationPreferences.hideRead = true
        notificationPreferences.excludedWikis.add("en")
        notificationPreferences.excludedTypes.add("type1")
        viewModel.updateSearchQuery("Header 1") // Will limit to headers starting with "Header 1"

        var lastResult: Any? = null // used for detecting identical emissions of flow

        // Wait for initialization and initial fetch to finish without blocking
        withTimeout(100000) {
            lastResult = viewModel.uiState.filter { it is Resource.Success }.first()
        }

        val times = mutableListOf<Long>()

        println("Starting performance measurement ($numberNotifications notifications, $numberIterations iterations)...")

        repeat(numberIterations) { iteration ->
            val time = measureTimeMillis {
                viewModel.fetchAndSave(refresh = true)
                val result = viewModel.uiState.filter {
                    it is Resource.Success && it !== lastResult
                }.first()
                lastResult = result
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
        override suspend fun fetchAndSave(filter: String?, continueStr: String?) = "dummy"

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
        override suspend fun getEndOfPaginationReachedFlow(): Flow<Boolean> {
            return flowOf(false) // not used in this test suite
        }
        override suspend fun clearRemoteKeys() {}
        override suspend fun insertNotifications(notificationList: List<Notification>) {}
        override suspend fun syncAll(filter: String) {}
    }

    private class FakeNotificationFilterHelper : NotificationFilterHelper {
        override fun allWikisList(): List<String> {
            return listOf("en", "zh", "dummy")
        }

        override fun allTypesIdList(): List<String> {
            return listOf("type1", "type2")
        }
    }
}
