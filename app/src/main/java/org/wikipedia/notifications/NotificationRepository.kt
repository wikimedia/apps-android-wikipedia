package org.wikipedia.notifications

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification

/**
 * Interface for notification-related data operations, coordinating between the local
 * Room database and the remote MediaWiki API.
 */
interface NotificationRepository {
    /**
     * Retrieves all notifications from the local database - used by legacy implementation
     */
    suspend fun getAllNotifications(): List<Notification>

    /**
     * Updates a notification's status (e.g., read/unread) in the local database.
     */
    suspend fun updateNotification(notification: Notification)

    /**
     * Discovers which wikis have unread notifications and maps their database names to [WikiSite]s.
     * Uses a single call to the primary wiki site with cross-wiki aggregation enabled.
     */
    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite>

    /**
     * Fetches a page of notifications from the remote API and persists them to the local database.
     * Supports pagination via the continue token.
     */
    suspend fun fetchAndSave(filter: String?, continueStr: String? = null): String?

    /**
     * Passes through the request of the view model to the local database.
     * Parameters are:
     * - ids: list of notification ids which shall be updated
     * - value: (null or a string containing a timestamp)
    */
    suspend fun markItemsAsRead(
        ids: List<Long>,
        readTimestamp: String?
    )

    /**
     * Retrieves all notifications from the local database with sorting and filtering:
     * - Sorting by timestamp
     * - if indicated by boolean flag (hideReadNotifications), filtering out read notifications
     * - if a search string is provided (searchQuery), check if any of these notification attributes
     * contains it:
     *   - title
     *   - header
     *   - body
     *   - (secondary) link label
     * - filtering out excluded types (excludedTypeCodes)
     * - filtering to included wikis (includedWikiCodes) where a list of language codes is provided
     *   and the query reproduces the StringUtil.dbNameToLangCode function using the wiki of the
     *   notification as input and compares it with the provided language codes
     * - if indicated by boolean flag (hideNotMentioned), filtering to those notifications where
     *   the category string is starting with one of the strings MENTIONS_GROUP list
     *
     *   (Deduplication is done when inserting new entries in the database)
     */
    fun getNotificationsFlow(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean
    ): Flow<PagingData<Notification>>

    fun getUnreadCountsFlow(
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>
    ): Flow<Pair<Int, Int>>

    suspend fun getRemoteKey(wiki: String): String?

    suspend fun clearRemoteKeys()

    suspend fun saveRemoteKey(wiki: String, nextContinueStr: String?)

    suspend fun getEndOfPaginationReachedFlow(): Flow<Boolean>
}
