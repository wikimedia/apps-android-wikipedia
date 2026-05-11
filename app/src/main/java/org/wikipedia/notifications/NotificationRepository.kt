package org.wikipedia.notifications

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification

/**
 * Interface for notification-related data operations, coordinating between the local
 * Room database and the remote MediaWiki API.
 */
interface NotificationRepository {
    /**
     * Retrieves all notifications from the local database.
     */
    suspend fun getAllNotifications(): List<Notification>

    /**
     * Updates a notification's status (e.g., read/unread) in the local database.
     */
    suspend fun updateNotification(notification: Notification)

    /**
     * Discovers which wikis have unread notifications and maps their database names to [WikiSite]s.
     * Uses a single call to the primary wiki site with cross-wiki aggregation enabled.
     */
    suspend fun fetchUnreadWikiDbNames(): Map<String, WikiSite>

    /**
     * Fetches a page of notifications from the remote API and persists them to the local database.
     * Supports pagination via the continue token.
     */
    suspend fun fetchAndSave(filter: String?, continueStr: String? = null): String?
}