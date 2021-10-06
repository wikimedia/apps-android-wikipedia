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

    @Query("SELECT * FROM Notification WHERE `wiki` = :wiki")
    fun getNotificationsByWiki(wiki: String): List<Notification>
}
