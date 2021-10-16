package org.wikipedia.notifications.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notifications: List<Notification>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("SELECT * FROM Notification")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    fun getNotificationsByWiki(wiki: List<String>): Flow<List<Notification>>
}
