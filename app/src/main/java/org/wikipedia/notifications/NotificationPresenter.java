package org.wikipedia.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;

public final class NotificationPresenter {
    private static final int REQUEST_CODE_PAGE = 0;
    private static final int REQUEST_CODE_HISTORY = 1;
    private static final int REQUEST_CODE_AGENT = 2;
    private static final String CHANNEL_ID = "GENERAL_CHANNEL";

    public static void showNotification(@NonNull Context context, Notification n) {
        @StringRes int title;
        Spanned description;
        @DrawableRes int icon;
        @ColorInt int color;

        Uri historyUri = uriForPath(n, "Special:History/"
                + (n.isFromWikidata() ? n.title().text() : n.title().full()));
        Uri agentUri = uriForPath(n, "User:" + n.agent().name());

        Intent pageIntent = PageActivity.newIntent(context, n.title().full());

        PendingIntent historyIntent = PendingIntent.getActivity(context, REQUEST_CODE_HISTORY,
                ShareUtil.createChooserIntent(new Intent(Intent.ACTION_VIEW, historyUri), null,
                        context), PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent agentIntent = PendingIntent.getActivity(context, REQUEST_CODE_AGENT,
                ShareUtil.createChooserIntent(new Intent(Intent.ACTION_VIEW, agentUri), null,
                        context), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        switch (n.type()) {
            case Notification.TYPE_EDIT_USER_TALK:
                description = StringUtil.fromHtml(context.getString(R.string.notification_talk, n.agent().name(), n.title().full()));
                icon = R.drawable.ic_chat_white_24dp;
                title = R.string.notification_talk_title;
                color = ContextCompat.getColor(context, R.color.accent50);
                builder.addAction(0, context.getString(R.string.notification_button_view_user), agentIntent);
                break;
            case Notification.TYPE_REVERTED:
                pageIntent.putExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER, n.title().text());
                description = StringUtil.fromHtml(context.getString(R.string.notification_reverted, n.agent().name(), n.title().full()));
                icon = R.drawable.ic_rotate_left_white_24dp;
                title = R.string.notification_reverted_title;
                color = ContextCompat.getColor(context, R.color.red50);
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                break;
            case Notification.TYPE_EDIT_THANK:
                description = StringUtil.fromHtml(context.getString(R.string.notification_thanks, n.agent().name(), n.title().full()));
                icon = R.drawable.ic_favorite_white_24dp;
                title = R.string.notification_thanks_title;
                color = ContextCompat.getColor(context, R.color.green50);
                builder.addAction(0, context.getString(R.string.notification_button_view_user), agentIntent);
                break;
            default:
                return;
        }

        builder.setContentIntent(PendingIntent.getActivity(context, REQUEST_CODE_PAGE, pageIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setContentText(description)
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        ? icon : R.mipmap.launcher)
                .setColor(color)
                .setContentTitle(context.getString(title));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(n.id(), builder.build());
    }

    private static Uri uriForPath(@NonNull Notification n, @NonNull String path) {
        return new Uri.Builder()
                .scheme(WikipediaApp.getInstance().getWikiSite().scheme())
                .authority(n.isFromWikidata() ? "m.wikidata.org" : WikipediaApp.getInstance().getWikiSite().mobileAuthority())
                .appendPath("wiki")
                .appendPath(path)
                .build();
    }

    private NotificationPresenter() {
    }
}
