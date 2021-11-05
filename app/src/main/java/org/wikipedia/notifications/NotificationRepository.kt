package org.wikipedia.notifications

import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

class NotificationRepository constructor(private val notificationDao: NotificationDao) {

    fun getAllNotifications() = notificationDao.getAllNotifications()

    suspend fun insertNotifications(notifications: List<Notification>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }

    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        val dbNameMap = mutableMapOf<String, WikiSite>()
        val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL)).unreadNotificationWikis()
        val wikiMap = response.query?.unreadNotificationWikis
        dbNameMap.clear()
        for (key in wikiMap!!.keys) {
            if (wikiMap[key]!!.source != null) {
                dbNameMap[key] = WikiSite(wikiMap[key]!!.source!!.base)
            }
        }
        return dbNameMap
    }

    suspend fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String? = null): String? {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotifications(wikiList, filter, continueStr)
        response.query?.notifications?.let {
            // TODO: maybe add a logic to avoid adding same data into database.
            insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr
    }
}
