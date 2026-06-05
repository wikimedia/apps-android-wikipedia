package org.wikipedia.notifications.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM Notification")
    suspend fun deleteAll()

    @Query("SELECT * FROM Notification ORDER BY timestamp DESC")
    suspend fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    suspend fun getNotificationsByWiki(wiki: List<String>): List<Notification>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki) AND `id` IN (:id)")
    suspend fun getNotificationById(wiki: String, id: Long): Notification?

    /**
     * Marks selected items as read or unread.
     * Parameters are:
     * - ids: list of notification ids which shall be updated
     * - value: (null or a string containing a timestamp)
     */
    @Query("""
        SELECT * FROM Notification
        WHERE ((:hideReadNotifications = 0) OR (read IS NULL))
        AND ((:searchQuery IS NULL) OR
             (LOWER(title) LIKE '%' || LOWER(:searchQuery) || '%') OR
             (LOWER(contents) LIKE '%' || LOWER(:searchQuery) || '%'))
        AND (
            (:excludeSystem = 0 OR category NOT LIKE 'system%') AND
            (:excludeMilestone = 0 OR category NOT LIKE 'thank-you-edit%') AND
            (:excludeUserTalk = 0 OR category NOT LIKE 'edit-user-talk%') AND
            (:excludeEditThank = 0 OR category NOT LIKE 'edit-thank%') AND
            (:excludeReverted = 0 OR category NOT LIKE 'reverted%') AND
            (:excludeLoginFail = 0 OR category NOT LIKE 'login-fail%') AND
            (:excludeMention = 0 OR category NOT LIKE 'mention%') AND
            (:excludeEmailUser = 0 OR category NOT LIKE 'emailuser%') AND
            (:excludeUserRights = 0 OR category NOT LIKE 'user-rights%') AND
            (:excludeArticleLinked = 0 OR category NOT LIKE 'article-linked%') AND
            (:excludeAlpha = 0 OR category NOT LIKE 'alpha-builder-checker%') AND
            (:excludeReadingList = 0 OR category NOT LIKE 'reading-list-syncing%') AND
            (:excludeSyncing = 0 OR category NOT LIKE 'syncing%') AND
            (:excludeRecommended = 0 OR category NOT LIKE 'recommended-reading-lists%') AND
            (:excludeGames = 0 OR category NOT LIKE 'games%')
        )
        AND (REPLACE(
            CASE WHEN wiki LIKE '%wiki' THEN SUBSTR(wiki, 1, LENGTH(wiki)-4) ELSE wiki END,
            '_', '-'
        ) IN (:includedWikiCodes))
        AND ((:hideNotMentioned = 0) OR
             (category LIKE 'mention%') OR
             (category LIKE 'edit-user-talk%') OR
             (category LIKE 'emailuser%') OR
             (category LIKE 'user-rights%') OR
             (category LIKE 'reverted%'))
        ORDER BY timestamp DESC
    """)
    fun getAllSelectedNotificationsPaged(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean,
        excludeSystem: Boolean,
        excludeMilestone: Boolean,
        excludeUserTalk: Boolean,
        excludeEditThank: Boolean,
        excludeReverted: Boolean,
        excludeLoginFail: Boolean,
        excludeMention: Boolean,
        excludeEmailUser: Boolean,
        excludeUserRights: Boolean,
        excludeArticleLinked: Boolean,
        excludeAlpha: Boolean,
        excludeReadingList: Boolean,
        excludeSyncing: Boolean,
        excludeRecommended: Boolean,
        excludeGames: Boolean
    ): PagingSource<Int, Notification>

    @Query("""
        UPDATE Notification
        SET read = :readTimeStamp
        WHERE id IN (:ids)
    """)
    suspend fun markItemsAsRead(
        ids: List<Long>,
        readTimeStamp: String?
    )

    @Query("""
        SELECT COUNT(*) FROM Notification
        WHERE (read IS NULL)
        AND (
            (:excludeSystem = 0 OR category NOT LIKE 'system%') AND
            (:excludeMilestone = 0 OR category NOT LIKE 'thank-you-edit%') AND
            (:excludeUserTalk = 0 OR category NOT LIKE 'edit-user-talk%') AND
            (:excludeEditThank = 0 OR category NOT LIKE 'edit-thank%') AND
            (:excludeReverted = 0 OR category NOT LIKE 'reverted%') AND
            (:excludeLoginFail = 0 OR category NOT LIKE 'login-fail%') AND
            (:excludeMention = 0 OR category NOT LIKE 'mention%') AND
            (:excludeEmailUser = 0 OR category NOT LIKE 'emailuser%') AND
            (:excludeUserRights = 0 OR category NOT LIKE 'user-rights%') AND
            (:excludeArticleLinked = 0 OR category NOT LIKE 'article-linked%') AND
            (:excludeAlpha = 0 OR category NOT LIKE 'alpha-builder-checker%') AND
            (:excludeReadingList = 0 OR category NOT LIKE 'reading-list-syncing%') AND
            (:excludeSyncing = 0 OR category NOT LIKE 'syncing%') AND
            (:excludeRecommended = 0 OR category NOT LIKE 'recommended-reading-lists%') AND
            (:excludeGames = 0 OR category NOT LIKE 'games%')
        )
        AND (REPLACE(
            CASE WHEN wiki LIKE '%wiki' THEN SUBSTR(wiki, 1, LENGTH(wiki)-4) ELSE wiki END,
            '_', '-'
        ) IN (:includedWikiCodes))
    """)
    fun getUnreadCount(
        includedWikiCodes: List<String>,
        excludeSystem: Boolean,
        excludeMilestone: Boolean,
        excludeUserTalk: Boolean,
        excludeEditThank: Boolean,
        excludeReverted: Boolean,
        excludeLoginFail: Boolean,
        excludeMention: Boolean,
        excludeEmailUser: Boolean,
        excludeUserRights: Boolean,
        excludeArticleLinked: Boolean,
        excludeAlpha: Boolean,
        excludeReadingList: Boolean,
        excludeSyncing: Boolean,
        excludeRecommended: Boolean,
        excludeGames: Boolean
    ): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM Notification
        WHERE (read IS NULL)
        AND (
            (:excludeSystem = 0 OR category NOT LIKE 'system%') AND
            (:excludeMilestone = 0 OR category NOT LIKE 'thank-you-edit%') AND
            (:excludeUserTalk = 0 OR category NOT LIKE 'edit-user-talk%') AND
            (:excludeEditThank = 0 OR category NOT LIKE 'edit-thank%') AND
            (:excludeReverted = 0 OR category NOT LIKE 'reverted%') AND
            (:excludeLoginFail = 0 OR category NOT LIKE 'login-fail%') AND
            (:excludeMention = 0 OR category NOT LIKE 'mention%') AND
            (:excludeEmailUser = 0 OR category NOT LIKE 'emailuser%') AND
            (:excludeUserRights = 0 OR category NOT LIKE 'user-rights%') AND
            (:excludeArticleLinked = 0 OR category NOT LIKE 'article-linked%') AND
            (:excludeAlpha = 0 OR category NOT LIKE 'alpha-builder-checker%') AND
            (:excludeReadingList = 0 OR category NOT LIKE 'reading-list-syncing%') AND
            (:excludeSyncing = 0 OR category NOT LIKE 'syncing%') AND
            (:excludeRecommended = 0 OR category NOT LIKE 'recommended-reading-lists%') AND
            (:excludeGames = 0 OR category NOT LIKE 'games%')
        )
        AND (REPLACE(
            CASE WHEN wiki LIKE '%wiki' THEN SUBSTR(wiki, 1, LENGTH(wiki)-4) ELSE wiki END,
            '_', '-'
        ) IN (:includedWikiCodes))
        AND (
             (category LIKE 'mention%') OR
             (category LIKE 'edit-user-talk%') OR
             (category LIKE 'emailuser%') OR
             (category LIKE 'user-rights%') OR
             (category LIKE 'reverted%')
        )
    """)
    fun getUnreadMentionsCount(
        includedWikiCodes: List<String>,
        excludeSystem: Boolean,
        excludeMilestone: Boolean,
        excludeUserTalk: Boolean,
        excludeEditThank: Boolean,
        excludeReverted: Boolean,
        excludeLoginFail: Boolean,
        excludeMention: Boolean,
        excludeEmailUser: Boolean,
        excludeUserRights: Boolean,
        excludeArticleLinked: Boolean,
        excludeAlpha: Boolean,
        excludeReadingList: Boolean,
        excludeSyncing: Boolean,
        excludeRecommended: Boolean,
        excludeGames: Boolean
    ): Flow<Int>
}
