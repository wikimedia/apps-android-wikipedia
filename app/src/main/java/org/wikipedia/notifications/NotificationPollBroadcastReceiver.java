package org.wikipedia.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.main.MainActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_GO_TO_SE_TAB;

public class NotificationPollBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_POLL = "action_notification_poll";
    private static final int MAX_LOCALLY_KNOWN_NOTIFICATIONS = 32;
    private static final int FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 3;
    private static final int SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY = 7;

    private Map<String, WikiSite> dbNameWikiSiteMap = new HashMap<>();
    private Map<String, String> dbNameWikiNameMap = new HashMap<>();
    private List<Long> locallyKnownNotifications;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            // To test the BOOT_COMPLETED intent:
            // `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

            // Update our channel name, if needed.
            L.v("channel=" + ReleaseUtil.getChannel(context));

            startPollTask(context);
        } else if (TextUtils.equals(intent.getAction(), ACTION_POLL)) {

            if (!AccountUtil.isLoggedIn()) {
                return;
            }

            maybeShowLocalNotificationForEditorReactivation(context);

            if (!Prefs.notificationPollEnabled()) {
                return;
            }

            locallyKnownNotifications = Prefs.getLocallyKnownNotifications();
            pollNotifications(context);
        }
    }

    public static void startPollTask(@NonNull Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        try {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    TimeUnit.MINUTES.toMillis(context.getResources().getInteger(R.integer.notification_poll_interval_minutes)
                            / (Prefs.isSuggestedEditsReactivationTestEnabled() && !ReleaseUtil.isDevRelease() ? 10 : 1)),
                    getAlarmPendingIntent(context));
        } catch (Exception e) {
            // There seems to be a Samsung-specific issue where it doesn't update the existing
            // alarm correctly and adds it as a new one, and eventually hits the limit of 500
            // concurrent alarms, causing a crash.
            L.e(e);
        }
    }

    public static void stopPollTask(@NonNull Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        alarmManager.cancel(getAlarmPendingIntent(context));
    }

    @NonNull private static PendingIntent getAlarmPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, NotificationPollBroadcastReceiver.class);
        intent.setAction(ACTION_POLL);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressLint("CheckResult")
    private void pollNotifications(@NonNull final Context context) {
        ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getLastUnreadNotification()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    String lastNotificationTime = "";
                    if (response.query().notifications().list() != null
                            && response.query().notifications().list().size() > 0) {
                        for (Notification n : response.query().notifications().list()) {
                            if (n.getUtcIso8601().compareTo(lastNotificationTime) > 0) {
                                lastNotificationTime = n.getUtcIso8601();
                            }
                        }
                    }
                    if (lastNotificationTime.compareTo(Prefs.getRemoteNotificationsSeenTime()) <= 0) {
                        // we're in sync!
                        return;
                    }
                    Prefs.setRemoteNotificationsSeenTime(lastNotificationTime);
                    retrieveNotifications(context);
                }, t -> {
                    if (t instanceof MwException && ((MwException)t).getError().getTitle().equals("login-required")) {
                        assertLoggedIn();
                    }
                    L.e(t);
                });
    }

    public void assertLoggedIn() {
        // Attempt to get a dummy CSRF token, which should automatically re-log us in explicitly,
        // and should automatically log us out if the credentials are no longer valid.
        Completable.fromAction(() -> new CsrfTokenClient(WikipediaApp.getInstance().getWikiSite(), WikipediaApp.getInstance().getWikiSite())
                .getTokenBlocking()).subscribeOn(Schedulers.io())
                .subscribe();
    }

    @SuppressLint("CheckResult")
    private void retrieveNotifications(@NonNull final Context context) {
        dbNameWikiSiteMap.clear();
        dbNameWikiNameMap.clear();
        ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getUnreadNotificationWikis()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    Map<String, Notification.UnreadNotificationWikiItem> wikiMap = response.query().unreadNotificationWikis();
                    List<String> wikis = new ArrayList<>();
                    wikis.addAll(wikiMap.keySet());
                    for (String dbName : wikiMap.keySet()) {
                        if (wikiMap.get(dbName).getSource() != null) {
                            dbNameWikiSiteMap.put(dbName, new WikiSite(wikiMap.get(dbName).getSource().getBase()));
                            dbNameWikiNameMap.put(dbName, wikiMap.get(dbName).getSource().getTitle());
                        }
                    }
                    getFullNotifications(context, wikis);
                }, L::e);
    }

    @SuppressLint("CheckResult")
    private void getFullNotifications(@NonNull final Context context, @NonNull List<String> foreignWikis) {
        ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getAllNotifications(foreignWikis.isEmpty() ? "*" : TextUtils.join("|", foreignWikis), "!read", null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> onNotificationsComplete(context, response.query().notifications().list()),
                        L::e);
    }

    private void onNotificationsComplete(@NonNull final Context context, @NonNull List<Notification> notifications) {
        if (notifications.isEmpty() || Prefs.isSuggestedEditsHighestPriorityEnabled()) {
            return;
        }
        boolean locallyKnownModified = false;
        List<Notification> knownNotifications = new ArrayList<>();
        List<Notification> notificationsToDisplay = new ArrayList<>();

        for (final Notification n : notifications) {
            knownNotifications.add(n);
            if (locallyKnownNotifications.contains(n.key())) {
                continue;
            }
            locallyKnownNotifications.add(n.key());
            if (locallyKnownNotifications.size() > MAX_LOCALLY_KNOWN_NOTIFICATIONS) {
                locallyKnownNotifications.remove(0);
            }
            notificationsToDisplay.add(n);
            locallyKnownModified = true;
        }

        if (notificationsToDisplay.size() > 2) {
            NotificationPresenter.showMultipleUnread(context, notificationsToDisplay.size());
        } else {
            for (final Notification n : notificationsToDisplay) {
                // TODO: remove these conditions when the time is right.
                if ((n.category().startsWith(Notification.CATEGORY_SYSTEM) && Prefs.notificationWelcomeEnabled())
                        || (n.category().equals(Notification.CATEGORY_EDIT_THANK) && Prefs.notificationThanksEnabled())
                        || (n.category().equals(Notification.CATEGORY_THANK_YOU_EDIT) && Prefs.notificationMilestoneEnabled())
                        || (n.category().equals(Notification.CATEGORY_REVERTED) && Prefs.notificationRevertEnabled())
                        || (n.category().equals(Notification.CATEGORY_EDIT_USER_TALK) && Prefs.notificationUserTalkEnabled())
                        || (n.category().equals(Notification.CATEGORY_LOGIN_FAIL) && Prefs.notificationLoginFailEnabled())
                        || (n.category().startsWith(Notification.CATEGORY_MENTION) && Prefs.notificationMentionEnabled())
                        || Prefs.showAllNotifications()) {

                    NotificationPresenter.showNotification(context, n, dbNameWikiNameMap.containsKey(n.wiki()) ? dbNameWikiNameMap.get(n.wiki()) : n.wiki());
                }
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
            markRead(wiki, notificationsPerWiki.get(wiki), false);
        }
    }

    public static void markRead(@NonNull WikiSite wiki, @NonNull List<Notification> notifications, boolean unread) {
        final String idListStr = TextUtils.join("|", notifications);
        CsrfTokenClient editTokenClient = new CsrfTokenClient(wiki, WikipediaApp.getInstance().getWikiSite());
        editTokenClient.request(new CsrfTokenClient.DefaultCallback() {
            @SuppressLint("CheckResult")
            @Override
            public void success(@NonNull String token) {
                ServiceFactory.get(wiki).markRead(token, unread ? null : idListStr, unread ? idListStr : null)
                        .subscribeOn(Schedulers.io())
                        .subscribe(response -> { }, L::e);
            }
        });
    }

    private void maybeShowLocalNotificationForEditorReactivation(@NonNull Context context) {
        if (Prefs.getLastDescriptionEditTime() == 0
                || WikipediaApp.getInstance().isAnyActivityResumed()) {
            return;
        }
        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.getLastDescriptionEditTime());
        if (Prefs.isSuggestedEditsReactivationTestEnabled()) {
            days = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Prefs.getLastDescriptionEditTime());
        }
        if (days >= FIRST_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && days < SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY
                && !Prefs.isSuggestedEditsReactivationPassStageOne()) {
            Prefs.setSuggestedEditsReactivationPassStageOne(true);
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_one);
        } else if (days >= SECOND_EDITOR_REACTIVATION_NOTIFICATION_SHOW_ON_DAY && Prefs.isSuggestedEditsReactivationPassStageOne()) {
            Prefs.setSuggestedEditsReactivationPassStageOne(false);
            showSuggestedEditsLocalNotification(context, R.string.suggested_edits_reactivation_notification_stage_two);
        }
    }

    public static void showSuggestedEditsLocalNotification(@NonNull Context context, @StringRes int description) {
        Intent intent = MainActivity.newIntent(context).putExtra(INTENT_EXTRA_GO_TO_SE_TAB, true);
        NotificationPresenter.showNotification(context, NotificationPresenter.getDefaultBuilder(context), 0,
                context.getString(R.string.suggested_edits_reactivation_notification_title),
                context.getString(description), context.getString(description),
                R.drawable.ic_mode_edit_white_24dp, R.color.accent50, false, intent);
    }
}
