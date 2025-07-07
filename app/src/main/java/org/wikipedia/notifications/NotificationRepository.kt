package org.wikipedia.notifications

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

class NotificationRepository(private val notificationDao: NotificationDao) {

    suspend fun getAllNotifications() = notificationDao.getAllNotifications()

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).unreadNotificationWikis()
        return response.query?.unreadNotificationWikis!!
            .mapNotNull { (key, wiki) -> wiki.source?.let { key to WikiSite(it.base) } }.toMap()
    }

    suspend fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String? = null): String? {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getAllNotifications(wikiList, filter, continueStr)
        response.query?.notifications?.let {
            notificationDao.insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr
    }
}
