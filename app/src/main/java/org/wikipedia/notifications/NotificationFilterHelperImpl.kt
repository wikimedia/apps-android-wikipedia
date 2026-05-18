package org.wikipedia.notifications

import org.wikipedia.settings.Prefs

/**
 * Concrete implementation of [NotificationFilterHelper] that allows mocking of static data of
 * [NotificationActivity].
 */class NotificationFilterHelperImpl: NotificationFilterHelper {
    override fun allWikisList(): List<String> {
        return NotificationFilterActivity.allWikisList()
    }

    override fun allTypesIdList(): List<String> {
        return NotificationFilterActivity.allTypesIdList()
    }
}