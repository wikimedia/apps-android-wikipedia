package org.wikipedia.notifications

/**
 * Interface for accessing notification-related user preferences.
 * Abstracts the underlying [org.wikipedia.settings.Prefs] for testability.
 */
interface NotificationPreferences {
    /**
     * Whether the user has opted to hide notifications that are already marked as read.
     */
    fun isHideReadNotificationsEnabled(): Boolean

    /**
     * Set of notification category codes that the user has opted to exclude from the inbox.
     */
    fun getNotificationExcludedTypeCodes(): Set<String>

    /**
     * Set of wiki database names (e.g., "enwiki") that the user has opted to exclude from the inbox.
     */
    fun getNotificationExcludedWikiCodes(): Set<String>
}
