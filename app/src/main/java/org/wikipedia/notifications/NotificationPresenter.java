package org.wikipedia.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;

public final class NotificationPresenter {
    private static final int REQUEST_CODE_ACTIVITY = 0;
    private static final int REQUEST_CODE_ACTION = 1;
    private static final String CHANNEL_ID = "MEDIAWIKI_ECHO_CHANNEL";

    public static void showNotification(@NonNull Context context, @NonNull Notification n, @NonNull String wikiSiteName) {
        String title;
        @DrawableRes int icon = R.drawable.ic_wikipedia_w;
        @ColorRes int color = R.color.base30;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.notification_echo_channel_description), importance);
                notificationChannel.setLightColor(ContextCompat.getColor(context, R.color.accent50));
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);


        title = StringUtil.fromHtml(n.getContents() != null ? n.getContents().getHeader() : "").toString();

        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getPrimary() != null) {
            addAction(context, builder, n.getContents().getLinks().getPrimary(), REQUEST_CODE_ACTION);
        }
        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getSecondary() != null && n.getContents().getLinks().getSecondary().size() > 0) {
            addAction(context, builder, n.getContents().getLinks().getSecondary().get(0), REQUEST_CODE_ACTION + 1);
        }
        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getSecondary() != null && n.getContents().getLinks().getSecondary().size() > 1) {
            addAction(context, builder, n.getContents().getLinks().getSecondary().get(1), REQUEST_CODE_ACTION + 2);
        }

        Intent activityIntent = NotificationActivity.newIntent(context);

        switch (n.type()) {
            case Notification.TYPE_EDIT_USER_TALK:
                icon = R.drawable.ic_chat_white_24dp;
                color = R.color.accent50;
                break;
            case Notification.TYPE_REVERTED:
                icon = R.drawable.ic_rotate_left_white_24dp;
                color = R.color.red50;
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                break;
            case Notification.TYPE_EDIT_THANK:
                icon = R.drawable.ic_usertalk_constructive;
                color = R.color.green50;
                break;
            case Notification.TYPE_EDIT_MILESTONE:
                icon = R.drawable.ic_mode_edit_white_24dp;
                color = R.color.accent50;
                break;
            default:
                break;
        }

        builder.setContentIntent(PendingIntent.getActivity(context, REQUEST_CODE_ACTIVITY, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLargeIcon(drawNotificationBitmap(context, color, icon))
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_wikipedia_w : R.mipmap.launcher)
                .setColor(ContextCompat.getColor(context, color))
                .setContentTitle(wikiSiteName)
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title));

        notificationManager.notify((int) n.key(), builder.build());
    }

    private static void addAction(Context context, NotificationCompat.Builder builder, Notification.Link link, int requestCode) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode,
                new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUrl())).putExtra(Constants.INTENT_EXTRA_VIEW_FROM_NOTIFICATION, true),
                PendingIntent.FLAG_UPDATE_CURRENT);
        String labelStr;
        if (!TextUtils.isEmpty(link.getTooltip())) {
            labelStr = StringUtil.fromHtml(link.getTooltip()).toString();
        } else {
            labelStr = StringUtil.fromHtml(link.getLabel()).toString();
        }
        builder.addAction(0, labelStr, pendingIntent);
    }

    private static Bitmap drawNotificationBitmap(@NonNull Context context, @ColorRes int color, @DrawableRes int icon) {
        final int bitmapHalfSize = DimenUtil.roundedDpToPx(20);
        final int iconHalfSize = DimenUtil.roundedDpToPx(12);
        Bitmap bmp = Bitmap.createBitmap(bitmapHalfSize * 2, bitmapHalfSize * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(context, color));
        canvas.drawCircle(bitmapHalfSize, bitmapHalfSize, bitmapHalfSize, p);
        Bitmap iconBmp = ResourceUtil.bitmapFromVectorDrawable(context, icon, android.R.color.white);
        canvas.drawBitmap(iconBmp, null, new Rect(bitmapHalfSize - iconHalfSize, bitmapHalfSize - iconHalfSize,
                bitmapHalfSize + iconHalfSize, bitmapHalfSize + iconHalfSize), null);
        iconBmp.recycle();
        return bmp;
    }

    private NotificationPresenter() {
    }
}
