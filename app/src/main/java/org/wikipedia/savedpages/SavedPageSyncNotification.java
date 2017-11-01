package org.wikipedia.savedpages;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.MathUtil;


public final class SavedPageSyncNotification{

    private static final SavedPageSyncNotification INSTANCE = new SavedPageSyncNotification();
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private Context mContext;
    private static final String CHANNEL_ID = "SYNCING_CHANNEL";
    private static final int NOTIFICATION_ID_FOR_SYNCING = 1001;
    private boolean syncCanceled;
    private boolean syncPaused;
    private boolean syncNotificationVisible;
    private int queueSize;
    private int syncCount;

    private SavedPageSyncNotification() {
        mContext = WikipediaApp.getInstance();
        mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static SavedPageSyncNotification getInstance() {
        return INSTANCE;
    }


    public void setCancelSyncDownload() {
        syncCanceled = true;
        doCancel();
        SavedPageSyncService.cancelService(mContext);
    }

    public void clearAll() {
        queueSize = 0;
        syncCount = 0;
        syncCanceled = false;
        syncPaused = false;
    }

    public void setPauseSyncDownload() {

        if (isSyncPaused()) {
            syncPaused = false;
            SavedPageSyncService.resumeService(mContext);
        } else {
            syncPaused = true;
            getInstance().show(false);
        }
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setVisible(boolean visible) {
        syncNotificationVisible = visible;
    }

    public boolean isVisible() {
        return syncNotificationVisible;
    }

    public boolean isSyncCanceled() {
        return syncCanceled;
    }

    public boolean isSyncPaused() {
        return syncPaused;
    }


    public void setup() {

        if (queueSize > 0) {
            int notificationIcon;
            String notificationTitle;
            String notificationDescription;
            String notificationInfo;

            // Notification channel ( >= API 26 )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = mContext.getString(R.string.notification_channel_name);
                String description = mContext.getString(R.string.notification_channel_description);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mChannel.setDescription(description);
                mChannel.setSound(null, null);
                mNotificationManager.createNotificationChannel(mChannel);
            }

            // setup notification content
            mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID);

            // build action buttons
            int pauseButton = R.string.notification_syncing_pause_button;
            int pauseButtonIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_pause_black_24dp : android.R.color.transparent;
            if (getInstance().isSyncPaused()) {
                pauseButton = R.string.notification_syncing_resume_button;
                pauseButtonIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_play_arrow_black_24dp : android.R.color.transparent;
            }

            // The reason why registering two receivers because the "addAction" has issues when the target classes are the same.
            // If the target classes are e.g.: "MainActivity.java", and we have put different data with different key,
            // The MainActivity can only receive the last data we have assigned.
            // e.g. assign PAUSE and then CANCEL. the action of both buttons will be assigned as CANCEL
            NotificationCompat.Action actionPause = actionBuilder(
                    SavedPageSyncBroadcastReceiver.SyncPauseReceiver.class,
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE,
                    pauseButtonIcon,
                    pauseButton);


            NotificationCompat.Action actionCancel = actionBuilder(
                    SavedPageSyncBroadcastReceiver.SyncCancelReceiver.class,
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_cancel_black_24dp : android.R.color.transparent,
                    R.string.notification_syncing_cancel_button);


            notificationIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_file_download_white_24dp : R.mipmap.launcher;
            notificationTitle = String.format(mContext.getString(R.string.notification_syncing_title), queueSize);
            notificationInfo = MathUtil.percentage(syncCount, queueSize) + "%";
            notificationDescription = String.format(mContext.getString(R.string.notification_syncing_description), queueSize - syncCount);


            mBuilder.setSmallIcon(notificationIcon)
                    .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), notificationIcon))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationDescription))
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationDescription)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(null)
                    .setContentInfo(notificationInfo)
                    .setOngoing(true)
                    .addAction(actionPause)
                    .addAction(actionCancel);

            // Reference: https://developer.android.com/reference/android/app/Notification.Builder.html#setContentInfo(java.lang.CharSequence)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBuilder.setSubText(notificationInfo);
            }
        }
    }


    public void show(boolean init) {
        if (queueSize > 0) {
            if (init && !isSyncCanceled() && !isSyncPaused()) {
                syncCount = 0;
                setup();
                mBuilder.setProgress(queueSize, syncCount, true);
                doNotify();
                syncCount++;
            } else {
                setup();
                if (!isSyncPaused()) {
                    syncCount++;
                }
                mBuilder.setProgress(queueSize, syncCount, false);
                doNotify();
                if (queueSize <= syncCount) {
                    doCancel();
                }
            }
        }
    }

    private void doCancel() {
        mNotificationManager.cancel(NOTIFICATION_ID_FOR_SYNCING);
    }

    private void doNotify() {
        if (isVisible() && !isSyncCanceled()) {
            mNotificationManager.notify(NOTIFICATION_ID_FOR_SYNCING, mBuilder.build());
        }
    }


    @NonNull private NotificationCompat.Action actionBuilder(@NonNull Class<?> targetClass,
                                                             @NonNull String intentExtra,
                                                             @NonNull int buttonDrawable,
                                                             @NonNull int buttonText) {
        return new NotificationCompat.Action.Builder(buttonDrawable, mContext.getString(buttonText), pendingIntentBuilder(targetClass, intentExtra, true)).build();
    }

    @NonNull private PendingIntent pendingIntentBuilder(@NonNull Class<?> targetClass,
                                                        @NonNull String intentExtra,
                                                        boolean isBroadcast) {
        Intent resultIntent = new Intent(mContext, targetClass);
        resultIntent.putExtra(intentExtra, true);

        if (!isBroadcast) {
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent resultPendingIntent = isBroadcast
                ? PendingIntent.getBroadcast(mContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                : PendingIntent.getActivity(mContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return resultPendingIntent;
    }
}
