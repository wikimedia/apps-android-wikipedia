package org.wikipedia.readinglist.sync;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.views.NotificationWithProgressBar;

public final class ReadingListSyncNotification {

    private static final ReadingListSyncNotification INSTANCE = new ReadingListSyncNotification();
    private static final String CHANNEL_ID = "READING_LIST_SYNCING_CHANNEL";
    private static final int NOTIFICATION_ID = 1002;
    private NotificationWithProgressBar notification;

    private ReadingListSyncNotification() {
        notification = new NotificationWithProgressBar();
        notification.setChannelId(CHANNEL_ID);
        notification.setNotificationId(NOTIFICATION_ID);
        notification.setChannelName(R.plurals.notification_syncing_reading_list_channel_name);
        notification.setChannelDescription(R.string.notification_syncing_reading_list_channel_description);
        notification.setNotificationIcon(android.R.drawable.ic_popup_sync);
        notification.setNotificationTitle(R.plurals.notification_syncing_reading_list_title);
        notification.setNotificationDescription(R.plurals.notification_syncing_reading_list_description);
    }

    public static ReadingListSyncNotification getInstance() {
        return INSTANCE;
    }

    public void setNotificationProgress(@NonNull Context context, int itemsTotal, int itemsProgress) {
        notification.setNotificationProgress(context, itemsTotal, itemsProgress);
    }

    public void cancelNotification(@NonNull Context context) {
        notification.cancelNotification(context);
    }
}
