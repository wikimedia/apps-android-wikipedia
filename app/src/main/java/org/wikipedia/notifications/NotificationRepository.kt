package org.wikipedia.notifications

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

class NotificationRepository(private val notificationDao: NotificationDao) {

    // Loads all notifications from database to memory
    suspend fun getAllNotifications() = notificationDao.getAllNotifications()

    // Updates one notification in the database
    // Used by the view model for managing the read/unread state of a notification
    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    // On launch of the view model, fetch all wiki sites which have notifications for the user
    // with a single API call to the user's primary wiki site.
    // The result is stored in a map inside the view model and used when marking items as read.
    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).unreadNotificationWikis()
        return response.query?.unreadNotificationWikis!!
            .mapNotNull { (key, wiki) -> wiki.source?.let { key to WikiSite(it.base) } }.toMap()
    }

    // Fetch all notifications (limited by paging size) with a single API call to the user's primary
    // wiki site applying
    // - filter (filter)
    // - pagination token (continueStr)
    // The result (in reverse chronological order) is stored in the database.
    suspend fun fetchAndSave(filter: String?, continueStr: String? = null): String? {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getAllNotifications(filter, continueStr)
        response.query?.notifications?.let {
            notificationDao.insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr
    }
}
