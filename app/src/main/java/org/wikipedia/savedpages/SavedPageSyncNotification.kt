package org.wikipedia.savedpages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.views.NotificationWithProgressBar

class SavedPageSyncNotification : BroadcastReceiver() {
    private val notification = NotificationWithProgressBar()

    init {
        notification.channelId = CHANNEL_ID
        notification.notificationId = NOTIFICATION_ID
        notification.channelName = R.plurals.notification_channel_name
        notification.channelDescription = R.string.notification_channel_description
        notification.notificationIcon = android.R.drawable.stat_sys_download
        notification.notificationTitle = R.plurals.notification_syncing_title
        notification.notificationDescription = R.plurals.notification_syncing_description
        notification.isEnableCancelButton = true
        notification.isEnablePauseButton = true
        notification.targetClass = SavedPageSyncNotification::class.java
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL, false)) {
            if (instance.isSyncPaused()) {
                SavedPageSyncService.enqueue()
            }
            instance.setSyncCanceled(true)
            instance.setSyncPaused(false)
        } else if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME, false)) {
            instance.setSyncCanceled(false)
            if (instance.isSyncPaused()) {
                instance.setSyncPaused(false)
                SavedPageSyncService.enqueue()
            } else {
                instance.setSyncPaused(true)
            }
        }
    }

    private fun setSyncPaused(paused: Boolean) {
        notification.isPaused = paused
    }

    fun setSyncCanceled(canceled: Boolean) {
        notification.isCanceled = canceled
    }

    fun isSyncCanceled(): Boolean {
        return notification.isCanceled
    }

    fun isSyncPaused(): Boolean {
        return notification.isPaused
    }

    fun setNotificationProgress(context: Context, itemsTotal: Int, itemsProgress: Int) {
        notification.setNotificationProgress(context, itemsTotal, itemsProgress)
    }

    fun setNotificationPaused(context: Context, itemsTotal: Int, itemsProgress: Int) {
        notification.setNotificationPaused(context, itemsTotal, itemsProgress)
    }

    fun cancelNotification(context: Context) {
        notification.cancelNotification(context)
    }

    companion object {
        private const val CHANNEL_ID = "SYNCING_CHANNEL"
        private const val NOTIFICATION_ID = 1001
        val instance = SavedPageSyncNotification()
    }
}
