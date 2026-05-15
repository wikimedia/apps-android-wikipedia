package org.wikipedia.notifications.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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
     * Retrieves all notifications from the local database with sorting and filtering:
     * - Sorting by timestamp (descending)
     * - Filtering out read notifications if [hideReadNotifications] is true
     * - Filtering by [searchQuery] against title, header, body, and secondary link labels
     * - Filtering out excluded categories in [excludedTypeCodes]
     * - Filtering to included wikis by transforming the database name to language code
     * - Filtering to only "mentions" if [hideNotMentioned] is true
     */
    @Query("""
        SELECT * FROM Notification
        WHERE ((:hideReadNotifications = 0) OR (read IS NULL))
        AND ((:searchQuery IS NULL) OR
             (title LIKE '%' || :searchQuery || '%') OR
             (contents LIKE '%' || :searchQuery || '%'))
        AND ((:hasExclusions = 0) OR (category NOT IN (:excludedTypeCodes)))
        AND (REPLACE(
            CASE WHEN wiki LIKE '%wiki' THEN SUBSTR(wiki, 1, LENGTH(wiki)-4) ELSE wiki END,
            '_', '-'
        ) IN (:includedWikiCodes))
        AND ((:hideNotMentioned = 0) OR
             (category IN (:mentionsGroup)) OR
             (((INSTR(category, '-') > 0) AND (SUBSTR(category, 1, INSTR(category, '-') - 1) IN (:mentionsGroup)))))
        ORDER BY timestamp DESC
    """)
    suspend fun getAllSelectedNotification(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        hasExclusions: Boolean,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean,
        mentionsGroup: List<String>
    ): List<Notification>

    /**
     * Marks selected items as read or unread.
     * Parameters are:
     * - ids: list of notification ids which shall be updated
     * - value: (null or a string containing a timestamp)
     */
    @Query("""
        UPDATE Notification
        SET read = :readTimeStamp
        WHERE id IN (:ids)
    """)
    suspend fun markItemsAsRead(
        ids: List<Long>,
        readTimeStamp: String?
    )
}