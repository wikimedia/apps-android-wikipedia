package org.wikipedia.notifications

class NotificationListItemContainer {
    val type: Int
    var notification: Notification? = null
    var selected = false

    constructor() {
        type = ITEM_SEARCH_BAR
    }

    constructor(notification: Notification) {
        this.notification = notification
        type = ITEM_NOTIFICATION
    }

    companion object {
        const val ITEM_SEARCH_BAR = 0
        const val ITEM_NOTIFICATION = 1
    }
}
