package org.wikipedia.notifications.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    // TODO: Adding suspend back once the Room library version is updated.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotifications(notifications: List<Notification>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateNotification(notification: Notification)

    @Delete
    fun deleteNotification(notification: Notification)

    @Query("SELECT * FROM Notification")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    fun getNotificationsByWiki(wiki: List<String>): Flow<List<Notification>>
}
