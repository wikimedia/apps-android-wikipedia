package org.wikipedia.notifications.db

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

    @Query("SELECT * FROM Notification")
    suspend fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    suspend fun getNotificationsByWiki(wiki: List<String>): Flow<List<Notification>>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki) AND `id` IN (:id)")
    suspend fun getNotificationById(wiki: String, id: Long): Notification?
}
