package org.wikipedia.notifications

import org.wikipedia.settings.Prefs

/**
 * Concrete implementation of [NotificationPreferences] that wraps the app's [Prefs].
 */
class NotificationPreferencesImpl : NotificationPreferences {
    override fun isHideReadNotificationsEnabled(): Boolean {
        return Prefs.hideReadNotificationsEnabled
    }

    override fun getNotificationExcludedTypeCodes(): Set<String> {
        return Prefs.notificationExcludedTypeCodes
    }

    override fun getNotificationExcludedWikiCodes(): Set<String> {
        return Prefs.notificationExcludedWikiCodes
    }
}
