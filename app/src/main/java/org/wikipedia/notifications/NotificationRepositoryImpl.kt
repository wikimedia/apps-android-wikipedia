package org.wikipedia.notifications

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.notifications.db.NotificationRemoteKey
import org.wikipedia.notifications.db.NotificationRemoteKeyDao

/**
 * Concrete implementation of [NotificationRepository] that uses Room and the MediaWiki API.
 */
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val remoteKeyDao: NotificationRemoteKeyDao
): NotificationRepository {
    private val mutex = Mutex()
    private var _endOfPaginationReached = MutableStateFlow(false)
    // Status is initialized with false because in the beginning we don't know how many notifications
    // the user has. Value will be loaded from database inside init
    var endOfPaginationReached = _endOfPaginationReached.asStateFlow()

    private var remoteKeyLoaded = false

    /* --- MOCK DATA GENERATOR FOR MANUAL TESTING ---
    private fun createFakeNotification(id: Long): Notification {
        val hour = (10 + (id % 12)).toInt().toString().padStart(2, '0')
        val json = """
            {
                "id": $id,
                "wiki": "enwiki",
                "category": "${if (id % 5 == 0L) "mention" else "edit-thank"}",
                "title": { "full": "Mock Notification $id" },
                "timestamp": { "utciso8601": "2023-10-27T$hour:00:00Z" },
                "*": {
                    "header": "Header for item $id",
                    "body": "This is a fake notification injected for testing look and feel. Total count: 1000 items. Current ID: $id."
                }
            }
        """.trimIndent()
        return JsonUtil.decodeFromString<Notification>(json)!!
    }

    companion object {
        // The repository is instantiated each time the user navigates to the notification screen.
        // For testing purposes, we want the subsequent deletion of the local database only to
        // during the first navigation to the notification page.
        private var isFirstInitialization = true
    }

    init {
        if (isFirstInitialization) {
            isFirstInitialization = false
            runBlocking {
                notificationDao.deleteAll()
            }
        }
    }
    // ----------------------------------------------*/

    /**
     * Retrieves ALL notifications from the database - used by legacy code
     */
    override suspend fun getAllNotifications() = notificationDao.getAllNotifications()

    /**
     * Updates one notification in the local database
     */
    override suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    /**
     * Retrieves a list of wiki sites which provide notification for the current user.
     * This function is called during instantiation of the view model.
    */
    override suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).unreadNotificationWikis()
        return response.query?.unreadNotificationWikis!!
            .mapNotNull { (key, wiki) -> wiki.source?.let { key to WikiSite(it.base) } }.toMap()
    }

    /**
     * Reads one page of notifications from the server and inserts them into the local database.
     * Wrapped in a Mutex to prevent concurrency issues between the Pager and background sync.
     */
    override suspend fun fetchAndSave(filter: String?, continueStr: String?): String? = mutex.withLock {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(
            WikipediaApp.instance.wikiSite).getAllNotifications(filter, continueStr)
        _endOfPaginationReached.value = response.continuation != null
        response.query?.notifications?.let {
            notificationDao.insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr

        /* --- OVERRIDE FOR MANUAL TESTING ---
        //Log.d("NotificationRepositoryImpl", "fetchAndSave called with continueStr=$continueStr")
        val totalMockItems = 250
        val pageSize = 50
        val currentOffset = continueStr?.toIntOrNull() ?: 0

        if (currentOffset >= totalMockItems) {
            _endOfPaginationReached.value = true
            return null
        }

        val mockPage = (currentOffset + 1..minOf(currentOffset + pageSize, totalMockItems)).map {
            createFakeNotification(it.toLong())
        }

        notificationDao.insertNotifications(mockPage)

        val nextOffset = currentOffset + pageSize
        val nextContinueStr = if (nextOffset < totalMockItems) nextOffset.toString() else null
        if (nextContinueStr == null) {
            _endOfPaginationReached.value = true
        }
        return nextContinueStr
        // ------------------------------------------------ */
    }

    /**
     * Marks a list of notifications (Ids provided) as read or unread depending on the
     * parameter
     */
    override suspend fun markItemsAsRead(ids: List<Long>, readTimestamp: String?) {
        notificationDao.markItemsAsRead(ids, readTimestamp)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getNotificationsFlow(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean
    ): Flow<PagingData<Notification>> {
        return Pager(
            config = PagingConfig(pageSize = 50),
            remoteMediator = NotificationRemoteMediator(this),
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

    /**
     * Retries the count of unread notifications from the local database.
     */
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

    override suspend fun getRemoteKey(wiki: String): String? {
        // return null if the database table is empty
        val notificationRemoteKey = remoteKeyDao.getRemoteKey(wiki) ?: return null
        _endOfPaginationReached.value = notificationRemoteKey.endOfPaginationReached
        remoteKeyLoaded = true
        return notificationRemoteKey.nextContinueStr
    }

    override suspend fun clearRemoteKeys() {
        remoteKeyLoaded = false
        remoteKeyDao.deleteAll()
    }

    override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) {
        // saveRemoteKey is called before a first call to getRemoteKey
        // in order to avoid overwriting endOfPaginationReached with invalid data,
        // retrieve it from local database before first save
        if (!remoteKeyLoaded) {
            getRemoteKey(wiki)
        }
        val newKey = NotificationRemoteKey(wiki, nextContinueStr, endOfPaginationReached.value)
        remoteKeyDao.insert(newKey)
    }

    /**
     * Provide the stateflow to the view model
     */
    override suspend fun getEndOfPaginationReachedFlow(): StateFlow<Boolean> {
        return endOfPaginationReached
    }

    // used by testing only
    override suspend fun insertNotifications(notificationList: List<Notification>) {}

    /**
     * Loads ALL notifications from the server to the database
     */
    override suspend fun syncAll(filter: String) {
        var currentToken: String? = getRemoteKey(Constants.NOTIFICATIONS_DB_REMOTE_KEY)

        // Safety check: only loop if we haven't reached the end yet
        if (endOfPaginationReached.value) return

        while (true) {
            // fetchAndSave performs the API call and the DB insert
            val nextToken = fetchAndSave(filter, currentToken) ?: break

            if (nextToken == currentToken) {
                // Prevent infinite loops if the API returns the same token
                break
            }

            currentToken = nextToken
            // Optional: add a small delay to avoid hitting API rate limits (e.g., 100ms)
            delay(Constants.NOTIFICATION_API_CALL_DELAY)
        }
    }
}
