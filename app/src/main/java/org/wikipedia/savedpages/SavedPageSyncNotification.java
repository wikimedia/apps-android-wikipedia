package org.wikipedia.savedpages;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.util.MathUtil;

public final class SavedPageSyncNotification extends BroadcastReceiver {

    private static final SavedPageSyncNotification INSTANCE = new SavedPageSyncNotification();
    private static final String CHANNEL_ID = "SYNCING_CHANNEL";
    private static final int NOTIFICATION_ID_FOR_SYNCING = 1001;
    private boolean syncCanceled;
    private boolean syncPaused;

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
        this.syncPaused = paused;
    }

    void setSyncCanceled(boolean canceled) {
        this.syncCanceled = canceled;
    }

    public boolean isSyncCanceled() {
        return syncCanceled;
    }

    public boolean isSyncPaused() {
        return syncPaused;
    }

    public void setNotificationProgress(@NonNull Context context, int itemsTotal, int itemsProgress) {
        syncCanceled = false;
        syncPaused = false;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        build(context, builder, itemsTotal, itemsProgress);
        builder.setProgress(itemsTotal, itemsProgress, itemsProgress == 0);
        showNotification(context, builder);
    }

    public void setNotificationPaused(@NonNull Context context, int itemsTotal, int itemsProgress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        build(context, builder, itemsTotal, itemsProgress);
        builder.setProgress(itemsTotal, itemsProgress, true);
        showNotification(context, builder);
    }

    private void build(@NonNull Context context, @NonNull NotificationCompat.Builder builder,
                       int total, int progress) {
        int notificationIcon;
        String notificationTitle;
        String notificationDescription;
        String notificationInfo;

        // Notification channel ( >= API 26 )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            mChannel.setSound(null, null);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(mChannel);
        }

        // build action buttons
        NotificationCompat.Action actionPause = actionBuilder(context,
                SavedPageSyncNotification.class,
                Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME,
                isSyncPaused() ? R.drawable.ic_play_arrow_black_24dp : R.drawable.ic_pause_black_24dp,
                isSyncPaused() ? R.string.notification_syncing_resume_button : R.string.notification_syncing_pause_button,
                0);

        NotificationCompat.Action actionCancel = actionBuilder(context,
                SavedPageSyncNotification.class,
                Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL,
                R.drawable.ic_cancel_black_24dp,
                R.string.notification_syncing_cancel_button,
                1);

        notificationIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_file_download_white_24dp : R.mipmap.launcher;
        notificationTitle = String.format(context.getString(R.string.notification_syncing_title), total);
        notificationInfo = (int)MathUtil.percentage(progress, total) + "%";
        notificationDescription = String.format(context.getString(R.string.notification_syncing_description), total - progress);

        builder.setSmallIcon(notificationIcon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), notificationIcon))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationDescription))
                .setContentTitle(notificationTitle)
                .setContentText(notificationDescription)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null)
                .setContentInfo(notificationInfo)
                .setOngoing(true)
                .addAction(actionPause)
                .addAction(actionCancel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(notificationInfo);
        }
    }

    public void cancelNotification(@NonNull Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(NOTIFICATION_ID_FOR_SYNCING);
    }

    private void showNotification(@NonNull Context context, @NonNull NotificationCompat.Builder builder) {
        if (!isSyncCanceled()) {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID_FOR_SYNCING, builder.build());
        }
    }

    @NonNull private NotificationCompat.Action actionBuilder(@NonNull Context context,
                                                             @NonNull Class<?> targetClass,
                                                             @NonNull String intentExtra,
                                                             @DrawableRes int buttonDrawable,
                                                             @StringRes int buttonText,
                                                             int requestCode) {
        return new NotificationCompat.Action.Builder(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? buttonDrawable : android.R.color.transparent,
                context.getString(buttonText),
                pendingIntentBuilder(context, targetClass, intentExtra, requestCode)).build();
    }

    @NonNull private PendingIntent pendingIntentBuilder(@NonNull Context context,
                                                        @NonNull Class<?> targetClass,
                                                        @NonNull String intentExtra,
                                                        int requestCode) {
        Intent resultIntent = new Intent(context, targetClass);
        resultIntent.putExtra(intentExtra, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getBroadcast(context, requestCode,
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
