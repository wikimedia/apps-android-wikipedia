package org.wikipedia.savedpages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.views.NotificationWithProgressBar;

public final class SavedPageSyncNotification extends BroadcastReceiver {

    private static final SavedPageSyncNotification INSTANCE = new SavedPageSyncNotification();
    private static final String CHANNEL_ID = "SYNCING_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private NotificationWithProgressBar notification;

    public SavedPageSyncNotification() {
        notification = new NotificationWithProgressBar();
        notification.setChannelId(CHANNEL_ID);
        notification.setNotificationId(NOTIFICATION_ID);
        notification.setChannelName(R.plurals.notification_channel_name);
        notification.setChannelDescription(R.string.notification_channel_description);
        notification.setNotificationIcon(android.R.drawable.stat_sys_download);
        notification.setNotificationTitle(R.plurals.notification_syncing_title);
        notification.setNotificationDescription(R.plurals.notification_syncing_description);
        notification.setEnableCancelButton(true);
        notification.setEnablePauseButton(true);
        notification.setTargetClass(SavedPageSyncNotification.class);
    }

    public static SavedPageSyncNotification getInstance() {
        return INSTANCE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL, false)) {
            if (getInstance().isSyncPaused()) {
                SavedPageSyncService.enqueue();
            }
            getInstance().setSyncCanceled(true);
            getInstance().setSyncPaused(false);
        } else if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME, false)) {
            getInstance().setSyncCanceled(false);
            if (getInstance().isSyncPaused()) {
                getInstance().setSyncPaused(false);
                SavedPageSyncService.enqueue();
            } else {
                getInstance().setSyncPaused(true);
            }
        }
    }

    void setSyncPaused(boolean paused) {
        notification.setPaused(paused);
    }

    void setSyncCanceled(boolean canceled) {
        notification.setCanceled(canceled);
    }

    public boolean isSyncCanceled() {
        return notification.isCanceled();
    }

    public boolean isSyncPaused() {
        return notification.isPaused();
    }

    public void setNotificationProgress(@NonNull Context context, int itemsTotal, int itemsProgress) {
        notification.setNotificationProgress(context, itemsTotal, itemsProgress);
    }

    public void setNotificationPaused(@NonNull Context context, int itemsTotal, int itemsProgress) {
        notification.setNotificationPaused(context, itemsTotal, itemsProgress);
    }

    public void cancelNotification(@NonNull Context context) {
        notification.cancelNotification(context);
    }
}
