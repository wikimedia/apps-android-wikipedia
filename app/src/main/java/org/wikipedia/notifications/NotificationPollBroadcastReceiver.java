package org.wikipedia.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NotificationPollBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_POLL = "action_notification_poll";
    private static final int MAX_LOCALLY_KNOWN_NOTIFICATIONS = 32;

    private NotificationClient client = new NotificationClient();
    private Map<String, WikiSite> dbNameWikiSiteMap = new HashMap<>();
    private Map<String, String> dbNameWikiNameMap = new HashMap<>();
    private List<Long> locallyKnownNotifications;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            startPollTask(context);
        } else if (TextUtils.equals(intent.getAction(), ACTION_POLL)) {
            if (!AccountUtil.isLoggedIn() || !Prefs.notificationPollEnabled()) {
                return;
            }

            locallyKnownNotifications = Prefs.getLocallyKnownNotifications();
            pollNotifications(context);
        }
    }

    public static void startPollTask(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                TimeUnit.MINUTES.toMillis(context.getResources().getInteger(R.integer.notification_poll_interval_minutes)),
                getAlarmPendingIntent(context));
    }

    public static void stopPollTask(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));
    }

    @NonNull private static PendingIntent getAlarmPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, NotificationPollBroadcastReceiver.class);
        intent.setAction(ACTION_POLL);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void pollNotifications(@NonNull final Context context) {
        client.getLastUnreadNotificationTime(WikipediaApp.getInstance().getWikiSite(), new NotificationClient.PollCallback() {
            @Override
            public void success(@NonNull String lastNotificationTime) {
                if (lastNotificationTime.compareTo(Prefs.getRemoteNotificationsSeenTime()) <= 0) {
                    // we're in sync!
                    return;
                }
                Prefs.setRemoteNotificationsSeenTime(lastNotificationTime);
                retrieveNotifications(context);
            }

            @Override
            public void failure(Throwable t) {
                L.e(t);
                // TODO
            }
        });
    }

    private void retrieveNotifications(@NonNull final Context context) {
        dbNameWikiSiteMap.clear();
        dbNameWikiSiteMap.clear();
        client.getUnreadNotificationWikis(WikipediaApp.getInstance().getWikiSite(), new NotificationClient.UnreadWikisCallback() {
            @Override
            public void success(@NonNull Map<String, Notification.UnreadNotificationWikiItem> wikiMap) {
                List<String> wikis = new ArrayList<>();
                wikis.addAll(wikiMap.keySet());
                for (String dbName : wikiMap.keySet()) {
                    if (wikiMap.get(dbName).getSource() != null) {
                        dbNameWikiSiteMap.put(dbName, new WikiSite(wikiMap.get(dbName).getSource().getBase()));
                        dbNameWikiNameMap.put(dbName, wikiMap.get(dbName).getSource().getTitle());
                    }
                }

                getFullNotifications(context, wikis);
            }

            @Override
            public void failure(Throwable t) {
                L.e(t);
                // TODO
            }
        });
    }

    private void getFullNotifications(@NonNull final Context context, @NonNull List<String> foreignWikis) {
        client.getAllNotifications(WikipediaApp.getInstance().getWikiSite(), new NotificationClient.Callback() {
            @Override
            public void success(@NonNull List<Notification> notifications, @Nullable String continueStr) {
                onNotificationsComplete(context, notifications);
            }

            @Override
            public void failure(Throwable t) {
                L.e(t);
                // TODO
            }
        }, false, null, foreignWikis.toArray(new String[0]));
    }


    private void onNotificationsComplete(@NonNull final Context context, @NonNull List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        boolean locallyKnownModified = false;
        List<Notification> knownNotifications = new ArrayList<>();

        for (final Notification n : notifications) {
            knownNotifications.add(n);
            if (locallyKnownNotifications.contains(n.key())) {
                continue;
            }
            locallyKnownNotifications.add(n.key());
            if (locallyKnownNotifications.size() > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
                locallyKnownNotifications.remove(0);
            }
            locallyKnownModified = true;

            // TODO: remove these conditions when the time is right.
            if ((n.type().equals(Notification.TYPE_WELCOME) && Prefs.notificationWelcomeEnabled())
                    || (n.type().equals(Notification.TYPE_EDIT_THANK) && Prefs.notificationThanksEnabled())
                    || (n.type().equals(Notification.TYPE_EDIT_MILESTONE) && Prefs.notificationMilestoneEnabled())
                    || Prefs.showAllNotifications()) {

                NotificationPresenter.showNotification(context, n, dbNameWikiNameMap.containsKey(n.wiki()) ? dbNameWikiNameMap.get(n.wiki()) : n.wiki());

            }
        }

        if (locallyKnownModified) {
            Prefs.setLocallyKnownNotifications(locallyKnownNotifications);
        }

        if (knownNotifications.size() > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
            markItemsAsRead(knownNotifications.subList(0, knownNotifications.size() - MAX_LOCALLY_KNOWN_NOTIFICATIONS));
        }
    }

    private void markItemsAsRead(List<Notification> items) {
        Map<WikiSite, List<Notification>> notificationsPerWiki = new HashMap<>();

        for (Notification item : items) {
            WikiSite wiki = dbNameWikiSiteMap.containsKey(item.wiki())
                    ? dbNameWikiSiteMap.get(item.wiki()) : WikipediaApp.getInstance().getWikiSite();
            if (!notificationsPerWiki.containsKey(wiki)) {
                notificationsPerWiki.put(wiki, new ArrayList<>());
            }
            notificationsPerWiki.get(wiki).add(item);
        }

        for (WikiSite wiki : notificationsPerWiki.keySet()) {
            client.markRead(wiki, notificationsPerWiki.get(wiki));
        }
    }
}
