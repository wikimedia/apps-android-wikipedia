package org.wikipedia.readinglist.sync

import android.content.Context
import org.wikipedia.R
import org.wikipedia.views.NotificationWithProgressBar

class ReadingListSyncNotification private constructor() {
    private val notification: NotificationWithProgressBar = NotificationWithProgressBar()

    init {
        notification.channelId = CHANNEL_ID
        notification.notificationId = NOTIFICATION_ID
        notification.channelName = R.plurals.notification_syncing_reading_list_channel_name
        notification.channelDescription = R.string.notification_syncing_reading_list_channel_description
        notification.notificationIcon = android.R.drawable.ic_popup_sync
        notification.notificationTitle = R.plurals.notification_syncing_reading_list_title
        notification.notificationDescription = R.plurals.notification_syncing_reading_list_description
    }

    fun setNotificationProgress(context: Context, itemsTotal: Int, itemsProgress: Int) {
        notification.setNotificationProgress(context, itemsTotal, itemsProgress)
    }

    fun cancelNotification(context: Context) {
        notification.cancelNotification(context)
    }

    companion object {
        val instance = ReadingListSyncNotification()
        private const val CHANNEL_ID = "READING_LIST_SYNCING_CHANNEL"
        private const val NOTIFICATION_ID = 1002
    }
}
