package org.wikipedia.notifications

class NotificationFilterHelperImpl: NotificationFilterHelper {
    override fun allWikisList(): List<String> {
        return NotificationFilterActivity.allWikisList()
    }

    override fun allTypesIdList(): List<String> {
        return NotificationFilterActivity.allTypesIdList()
    }
}