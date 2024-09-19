package org.wikipedia.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

class NotificationRepository(private val notificationDao: NotificationDao) {

    fun getAllNotifications() = notificationDao.getAllNotifications()

    private fun insertNotifications(notifications: List<Notification>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        return withContext(Dispatchers.IO) {
            val response = ServiceFactory.get(Constants.commonsWikiSite).unreadNotificationWikis()
            response.query?.unreadNotificationWikis?.mapNotNull {
                (key, wiki) -> wiki.source?.let { key to WikiSite(it.base) }
            }?.toMap() ?: emptyMap()
        }
    }

    suspend fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String? = null): String? {
        return withContext(Dispatchers.IO) {
            var newContinueStr: String? = null
            val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getAllNotifications(wikiList, filter, continueStr)
            response.query?.notifications?.let {
                insertNotifications(it.list.orEmpty())
                newContinueStr = it.continueStr
            }
            newContinueStr
        }
    }
}
