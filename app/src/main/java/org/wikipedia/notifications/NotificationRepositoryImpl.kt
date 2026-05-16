package org.wikipedia.notifications

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.runBlocking
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

    // --- MOCK DATA GENERATOR FOR MANUAL TESTING ---
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

    init {
        runBlocking {
            notificationDao.deleteAll()
        }
    }
    // ----------------------------------------------

    // currently used only by legacy code
    // @todo: check if it can be replaced
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
     * Reads one page of notifications from the server and inserts them into the local database
     */
    override suspend fun fetchAndSave(filter: String?, continueStr: String?): String? {
        /* var newContinueStr: String? = null
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getAllNotifications(filter, continueStr)
        Log.d("NotificationRepositoryImpl", "response from API call is $response")
        response.query?.notifications?.let {
            Log.d("NotificationRepositoryImpl", "inserting into local DB: ${it.list.orEmpty()}")
            notificationDao.insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr */
        // --- OVERRIDE FOR MANUAL TESTING (1000 ITEMS) ---
        val totalMockItems = 250
        val pageSize = 50
        val currentOffset = continueStr?.toIntOrNull() ?: 0
        
        if (currentOffset >= totalMockItems) return null

        val mockPage = (currentOffset + 1..minOf(currentOffset + pageSize, totalMockItems)).map { 
            createFakeNotification(it.toLong()) 
        }
        
        notificationDao.insertNotifications(mockPage)
        
        val nextOffset = currentOffset + pageSize
        return if (nextOffset < totalMockItems) nextOffset.toString() else null
        // ------------------------------------------------
    }

    // @todo: Used for testing/legacy: check if it can be replaced.
    override suspend fun getAllSelectedNotifications(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean
    ): List<Notification> {
        return emptyList()
    }

    /**
     * Marks a list of notifications (Ids provided) as read or unread depending on the
     * parameter
     */
    override suspend fun markItemsAsRead(ids: List<Long>, readTimestamp: String?) {
        notificationDao.markItemsAsRead(ids, readTimestamp)
    }

    /**
     * Reads a selection of notifications from the local database
     */
    @OptIn(ExperimentalPagingApi::class)
    override fun getNotificationsFlow(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean
    ): Flow<PagingData<Notification>> {
        Log.d("NotificationRepositoryImpl", "getNotificationsFlow called with $excludedTypeCodes")
        return Pager(
            config = PagingConfig(pageSize = 50),
            remoteMediator = NotificationRemoteMediator(this),
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

    /**
     * Retries the count of unread notifications from the local database.
     */
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

    override suspend fun getRemoteKey(wiki: String): String? {
        return remoteKeyDao.getRemoteKey(wiki)?.nextContinueStr
    }

    override suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?) {
        remoteKeyDao.insert(NotificationRemoteKey(wiki, nextContinueStr))
    }

    override suspend fun clearRemoteKeys() {
        remoteKeyDao.deleteAll()
    }
}
