package org.wikipedia.views;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

public class NotificationWithProgressBar {
    private boolean canceled;
    private boolean paused;
    private String channelId;
    private int notificationId;
    private int channelName;
    private int channelDescription;
    private int notificationIcon;
    private int notificationTitle;
    private int notificationDescription;
    private boolean enableCancelButton;
    private boolean enablePauseButton;
    private Class<?> targetClass;

    public void setNotificationProgress(@NonNull Context context, int itemsTotal, int itemsProgress) {
        canceled = false;
        paused = false;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getChannelId());
        build(context, builder, itemsTotal, itemsProgress);
        builder.setProgress(itemsTotal, itemsProgress, itemsProgress == 0);
        showNotification(context, builder);
    }

    public void setNotificationPaused(@NonNull Context context, int itemsTotal, int itemsProgress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getChannelId());
        build(context, builder, itemsTotal, itemsProgress);
        builder.setProgress(itemsTotal, itemsProgress, true);
        showNotification(context, builder);
    }

    private void build(@NonNull Context context, @NonNull NotificationCompat.Builder builder,
                       int total, int progress) {
        int builderIcon;
        String builderTitle;
        String builderDescription;
        String builderInfo;

        // Notification channel ( >= API 26 )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getResources().getQuantityString(getChannelName(), total);
            String description = context.getString(getChannelDescription());
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(getChannelId(), name, importance);
            mChannel.setDescription(description);
            mChannel.setSound(null, null);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(mChannel);
        }

        builderIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? getNotificationIcon() : R.mipmap.launcher;
        builderTitle = String.format(context.getResources().getQuantityString(getNotificationTitle(), total), total);
        builderInfo = (int) MathUtil.percentage(progress, total) + "%";
        builderDescription = String.format(context.getResources().getQuantityString(getNotificationDescription(), total - progress), total - progress);

        builder.setSmallIcon(builderIcon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), builderIcon))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(builderDescription))
                .setContentTitle(builderTitle)
                .setContentText(builderDescription)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null)
                .setContentInfo(builderInfo)
                .setOngoing(true);

        // build action buttons
        if (isEnablePauseButton()) {
            NotificationCompat.Action actionPause = actionBuilder(context,
                    getTargetClass(),
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE_RESUME,
                    isPaused() ? R.drawable.ic_play_arrow_black_24dp : R.drawable.ic_pause_black_24dp,
                    isPaused() ? R.string.notification_syncing_resume_button : R.string.notification_syncing_pause_button,
                    0);
            builder.addAction(actionPause);
        }

        if (isEnableCancelButton()) {
            NotificationCompat.Action actionCancel = actionBuilder(context,
                    getTargetClass(),
                    Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL,
                    R.drawable.ic_cancel_black_24dp,
                    R.string.notification_syncing_cancel_button,
                    1);
            builder.addAction(actionCancel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSubText(builderInfo);
        }
    }

    public void cancelNotification(@NonNull Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(getNotificationId());
    }

    private void showNotification(@NonNull Context context, @NonNull NotificationCompat.Builder builder) {
        if (!isCanceled()) {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(getNotificationId(), builder.build());
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

    private int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    private int getChannelName() {
        return channelName;
    }

    public void setChannelName(int channelName) {
        this.channelName = channelName;
    }

    private int getChannelDescription() {
        return channelDescription;
    }

    public void setChannelDescription(int channelDescription) {
        this.channelDescription = channelDescription;
    }

    private int getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(int notificationIcon) {
        this.notificationIcon = notificationIcon;
    }

    private int getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(int notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    private int getNotificationDescription() {
        return notificationDescription;
    }

    public void setNotificationDescription(int notificationDescription) {
        this.notificationDescription = notificationDescription;
    }

    private boolean isEnableCancelButton() {
        return enableCancelButton;
    }

    public void setEnableCancelButton(boolean enableCancelButton) {
        this.enableCancelButton = enableCancelButton;
    }

    private boolean isEnablePauseButton() {
        return enablePauseButton;
    }

    public void setEnablePauseButton(boolean enablePauseButton) {
        this.enablePauseButton = enablePauseButton;
    }

    private Class<?> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isPaused() {
        return paused;
    }
}
