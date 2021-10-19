package org.wikipedia.notifications

import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
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

    suspend fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String? = null): String? {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotificationsKT(wikiList, filter, continueStr)
        response.query?.notifications?.let {
            // TODO: maybe add a logic to avoid adding same data into database.
            insertNotification(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr
    }
}
