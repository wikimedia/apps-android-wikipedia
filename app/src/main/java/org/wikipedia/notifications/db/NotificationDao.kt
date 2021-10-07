package org.wikipedia.notifications.db

import androidx.room.*

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotification(notification: Notification)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateNotification(notification: Notification)

    @Delete
    fun deleteNotification(notification: Notification)

    @Query("SELECT * FROM Notification")
    fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM Notification WHERE `wiki` IN (:wiki)")
    fun getNotificationsByWiki(wiki: List<String>): List<Notification>
}
