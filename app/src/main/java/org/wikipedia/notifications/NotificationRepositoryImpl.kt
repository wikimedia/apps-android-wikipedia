package org.wikipedia.notifications

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao

/**
 * Concrete implementation of [NotificationRepository] that uses Room and the MediaWiki API.
 */
class NotificationRepositoryImpl(private val notificationDao: NotificationDao): NotificationRepository {

    override suspend fun getAllNotifications() = notificationDao.getAllNotifications()

    override suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    override suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite> {
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).unreadNotificationWikis()
        return response.query?.unreadNotificationWikis!!
            .mapNotNull { (key, wiki) -> wiki.source?.let { key to WikiSite(it.base) } }.toMap()
    }

    override suspend fun fetchAndSave(filter: String?, continueStr: String?): String? {
        var newContinueStr: String? = null
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getAllNotifications(filter, continueStr)
        response.query?.notifications?.let {
            notificationDao.insertNotifications(it.list.orEmpty())
            newContinueStr = it.continueStr
        }
        return newContinueStr
    }

    override suspend fun getAllSelectedNotifications(
        hideReadNotifications: Boolean,
        searchQuery: String?,
        excludedTypeCodes: Set<String>,
        includedWikiCodes: List<String>,
        hideNotMentioned: Boolean
    ): List<Notification> {
        return emptyList()
    }
}
