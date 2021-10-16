package org.wikipedia.notifications

import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

class NotificationRepository constructor(private val notificationDao: NotificationDao) {

    fun getAllNotifications() = notificationDao.getAllNotifications()

    fun getNotificationsByWiki(wiki: List<String>) = notificationDao.getNotificationsByWiki(wiki)

    suspend fun insertNotification(notifications: List<Notification>) {
        notificationDao.insertNotification(notifications)
    }

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }
}
