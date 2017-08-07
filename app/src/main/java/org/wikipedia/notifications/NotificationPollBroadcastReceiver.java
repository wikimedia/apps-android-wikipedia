package org.wikipedia.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;
import org.wikipedia.wikidata.EntityClient;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationPollBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_POLL = "action_notification_poll";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ACTION_POLL)) {

            if (AccountUtil.isLoggedIn()
                    && lastDescriptionEditedWithin(context.getResources()
                    .getInteger(R.integer.notification_poll_timeout_days))) {
                pollNotifications(context);
            } else {
                stopPollTask(context);
            }

        }
    }

    public void startPollTask(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                TimeUnit.MINUTES.toMillis(context.getResources().getInteger(R.integer.notification_poll_interval_minutes)),
                getAlarmPendingIntent(context));
    }

    public void stopPollTask(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));
    }

    @NonNull private PendingIntent getAlarmPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, NotificationPollBroadcastReceiver.class);
        intent.setAction(ACTION_POLL);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void pollNotifications(@NonNull final Context context) {
        NotificationClient.instance().getNotifications(new NotificationClient.Callback() {
            @Override
            public void success(@NonNull List<Notification> notifications) {
                if (notifications.isEmpty()) {
                    return;
                }

                // Mark these notifications as read, so that they won't appear again.
                NotificationClient.instance().markRead(notifications);

                for (final Notification n : notifications) {

                    // If the notification came from Wikidata, we need to resolve the Q-number
                    // title into the corresponding human-readable label.
                    if (n.isFromWikidata() && n.title().isMainNamespace()) {
                        EntityClient.instance().getLabelForLang(n.title().full(),
                                WikipediaApp.getInstance().getWikiSite().languageCode(),
                                new EntityClient.LabelCallback() {
                                    @Override
                                    public void success(@NonNull String label) {
                                        n.title().setFull(label);
                                        NotificationPresenter.showNotification(context, n);
                                    }

                                    @Override
                                    public void failure(@NonNull Throwable t) {
                                        L.e(t);
                                        // Show the notification anyway, but with unresolved ID.
                                        NotificationPresenter.showNotification(context, n);
                                    }
                                });
                    } else {
                        NotificationPresenter.showNotification(context, n);
                    }
                }
            }

            @Override
            public void failure(Throwable t) {
                L.e(t);
            }
        }, EntityClient.WIKIDATA_WIKI);
    }

    private boolean lastDescriptionEditedWithin(int days) {
        return new Date().getTime() - Prefs.getLastDescriptionEditTime() < TimeUnit.DAYS.toMillis(days);
    }
}
