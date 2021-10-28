package org.wikipedia.notifications

import org.wikipedia.notifications.db.Notification

class NotificationListItemContainer(
    val notification: Notification? = null,
    val type: Int = if (notification == null) ITEM_SEARCH_BAR else ITEM_NOTIFICATION,
    var selected: Boolean = false
) {
    companion object {
        const val ITEM_SEARCH_BAR = 0
        const val ITEM_NOTIFICATION = 1
    }
}
