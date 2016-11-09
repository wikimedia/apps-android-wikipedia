package org.wikipedia.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.login.User;
import org.wikipedia.util.log.L;
import org.wikipedia.wikidata.EntityClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationPollBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_POLL = "action_notification_poll";

    private static final long POLL_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ACTION_POLL)) {
            pollNotifications(context);
        }
    }

    public void startPollTask(@NonNull Context context) {
        Intent alarmIntent = new Intent(context, NotificationPollBroadcastReceiver.class);
        boolean isAlarmUp = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
        if (!isAlarmUp) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmIntent.setAction(ACTION_POLL);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL_MILLIS, pendingIntent);
        }
    }

    public void stopPollTask(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, NotificationPollBroadcastReceiver.class);
        alarmIntent.setAction(ACTION_POLL);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        alarmManager.cancel(pendingIntent);
    }

    private void pollNotifications(@NonNull final Context context) {
        if (!User.isLoggedIn()) {
            return;
        }

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
                                    public void failure(Throwable t) {
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
}
