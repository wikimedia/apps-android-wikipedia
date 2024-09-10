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
    fun insertNotifications(notifications: List<Notification>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM Notification")
    fun deleteAll()

    @Query("SELECT * FROM Notification")
    fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    fun getNotificationsByWiki(wiki: List<String>): Flow<List<Notification>>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki) AND `id` IN (:id)")
    fun getNotificationById(wiki: String, id: Long): Notification?
}
