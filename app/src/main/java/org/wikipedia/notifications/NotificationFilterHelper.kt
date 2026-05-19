package org.wikipedia.notifications

/**
 * Interface for accessing notification-related global data.
 * Abstracts the lists provided by [NotificationActivity] for testability.
 */
interface NotificationFilterHelper {
    fun allWikisList(): List<String>
    fun allTypesIdList(): List<String>
}
