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
import android.text.TextUtils;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.talk.TalkTopicsActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;

public final class NotificationPresenter {
    private static final String CHANNEL_ID = "MEDIAWIKI_ECHO_CHANNEL";

    public static void showNotification(@NonNull Context context, @NonNull Notification n, @NonNull String wikiSiteName) {
        String title;
        @DrawableRes int iconResId = R.drawable.ic_speech_bubbles;
        @ColorRes int iconColor = R.color.accent50;

        NotificationCompat.Builder builder = getDefaultBuilder(context, n.id(), n.type());

        title = StringUtil.fromHtml(n.getContents() != null ? n.getContents().getHeader() : "").toString();

        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getPrimary() != null) {
            if (Notification.CATEGORY_EDIT_USER_TALK.equals(n.category())) {
                addActionForTalkPage(context, builder, n.getContents().getLinks().getPrimary(), n);
            } else {
                addAction(context, builder, n.getContents().getLinks().getPrimary(), n);
            }
        }
        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getSecondary() != null && n.getContents().getLinks().getSecondary().size() > 0) {
            addAction(context, builder, n.getContents().getLinks().getSecondary().get(0), n);
        }
        if (n.getContents() != null && n.getContents().getLinks() != null
                && n.getContents().getLinks().getSecondary() != null && n.getContents().getLinks().getSecondary().size() > 1) {
            addAction(context, builder, n.getContents().getLinks().getSecondary().get(1), n);
        }

        Intent activityIntent = addIntentExtras(NotificationActivity.newIntent(context), n.id(), n.type());

        String s = n.category();
        if (Notification.CATEGORY_EDIT_USER_TALK.equals(s)) {
            iconResId = R.drawable.ic_edit_user_talk;
            iconColor = R.color.accent50;
        } else if (Notification.CATEGORY_REVERTED.equals(s)) {
            iconResId = R.drawable.ic_revert;
            iconColor = R.color.base20;
        } else if (Notification.CATEGORY_EDIT_THANK.equals(s)) {
            iconResId = R.drawable.ic_user_talk;
            iconColor = R.color.green50;
        } else if (Notification.CATEGORY_THANK_YOU_EDIT.equals(s)) {
            iconResId = R.drawable.ic_edit_progressive;
            iconColor = R.color.accent50;
        } else if (s.startsWith(Notification.CATEGORY_MENTION)) {
            iconResId = R.drawable.ic_mention;
            iconColor = R.color.accent50;
        } else if (Notification.CATEGORY_LOGIN_FAIL.equals(s)) {
            iconResId = R.drawable.ic_user_avatar;
            iconColor = R.color.base0;
        }

        showNotification(context, builder, (int) n.key(), wikiSiteName, title, title, iconResId, iconColor, true, activityIntent);
    }

    public static void showMultipleUnread(@NonNull Context context, int unreadCount) {
        // When showing the multiple-unread notification, we pass the unreadCount as the "id"
        // purely for analytics purposes, to get a sense of how many unread notifications are
        // typically queued up when the user has more than two of them.
        NotificationCompat.Builder builder = getDefaultBuilder(context, unreadCount, NotificationPollBroadcastReceiver.TYPE_MULTIPLE);
        showNotification(context, builder, 0, context.getString(R.string.app_name),
                context.getString(R.string.notification_many_unread, unreadCount), context.getString(R.string.notification_many_unread, unreadCount),
                R.drawable.ic_notifications_black_24dp, R.color.accent50, true,
                addIntentExtras(NotificationActivity.newIntent(context), unreadCount, NotificationPollBroadcastReceiver.TYPE_MULTIPLE));
    }

    public static Intent addIntentExtras(@NonNull Intent intent, long id, @NonNull String type) {
        return intent.putExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, id)
                .putExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE, type);
    }

    public static NotificationCompat.Builder getDefaultBuilder(@NonNull Context context, long id, String type) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDeleteIntent(NotificationPollBroadcastReceiver.getCancelNotificationPendingIntent(context, id, type));
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public static void showNotification(@NonNull Context context, @NonNull NotificationCompat.Builder builder, int id,
                                        @NonNull String title, @NonNull String text, @NonNull CharSequence longText,
                                        @DrawableRes int icon, @ColorRes int color, boolean drawIconCircle, @NonNull Intent bodyIntent) {
        builder.setContentIntent(PendingIntent.getActivity(context, 0, bodyIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLargeIcon(drawNotificationBitmap(context, color, icon, drawIconCircle))
                .setSmallIcon(R.drawable.ic_wikipedia_w)
                .setColor(ContextCompat.getColor(context, color))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(longText));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private static void addAction(Context context, NotificationCompat.Builder builder, Notification.Link link, Notification n) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                addIntentExtras(new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUrl())), n.id(), n.type()), 0);
        String labelStr;
        if (!TextUtils.isEmpty(link.getTooltip())) {
            labelStr = StringUtil.fromHtml(link.getTooltip()).toString();
        } else {
            labelStr = StringUtil.fromHtml(link.getLabel()).toString();
        }
        builder.addAction(0, labelStr, pendingIntent);
    }

    private static void addActionForTalkPage(Context context, NotificationCompat.Builder builder, Notification.Link link, Notification n) {
        WikiSite wiki = new WikiSite(link.getUrl());
        PageTitle title = wiki.titleForUri(Uri.parse(link.getUrl()));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                addIntentExtras(TalkTopicsActivity.newIntent(context, title.pageTitleForTalkPage()), n.id(), n.type()), 0);
        builder.addAction(0, StringUtil.fromHtml(link.getLabel()).toString(), pendingIntent);
    }

    private static Bitmap drawNotificationBitmap(@NonNull Context context, @ColorRes int color, @DrawableRes int icon, boolean drawIconCircle) {
        final int bitmapHalfSize = DimenUtil.roundedDpToPx(20);
        final int iconHalfSize = DimenUtil.roundedDpToPx(12);
        Bitmap bmp = Bitmap.createBitmap(bitmapHalfSize * 2, bitmapHalfSize * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(context, drawIconCircle ? color : android.R.color.transparent));
        canvas.drawCircle(bitmapHalfSize, bitmapHalfSize, bitmapHalfSize, p);
        Bitmap iconBmp = ResourceUtil.bitmapFromVectorDrawable(context, icon, drawIconCircle ? android.R.color.white : color);
        canvas.drawBitmap(iconBmp, null, new Rect(bitmapHalfSize - iconHalfSize, bitmapHalfSize - iconHalfSize,
                bitmapHalfSize + iconHalfSize, bitmapHalfSize + iconHalfSize), null);
        iconBmp.recycle();
        return bmp;
    }

    private NotificationPresenter() {
    }
}
