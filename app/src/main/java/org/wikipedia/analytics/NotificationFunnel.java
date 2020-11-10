package org.wikipedia.analytics;

import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.notifications.Notification;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;

public class NotificationFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppNotificationInteraction";
    private static final int REV_ID = 18325732;

    private static final int ACTION_CLICKED = 10;
    private static final int ACTION_DISMISSED = 11;

    private final long id;
    private final String wiki;
    private final String type;

    public static void processIntent(@NonNull Intent intent) {
        if (!intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID)) {
            return;
        }
        NotificationFunnel funnel = new NotificationFunnel(WikipediaApp.getInstance(),
                intent.getLongExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID, 0),
                WikipediaApp.getInstance().getWikiSite().dbName(),
                intent.getStringExtra(Constants.INTENT_EXTRA_NOTIFICATION_TYPE));
        if (TextUtils.equals(NotificationPollBroadcastReceiver.ACTION_CANCEL, intent.getAction())) {
            funnel.logDismissed();
        } else {
            funnel.logClicked();
        }
    }

    public NotificationFunnel(WikipediaApp app, Notification notification) {
        this(app, notification.id(), notification.wiki(), notification.type());
    }

    public NotificationFunnel(WikipediaApp app, long id, String wiki, String type) {
        super(app, SCHEMA_NAME, REV_ID);
        this.id = id;
        this.wiki = wiki;
        this.type = type;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "notification_id", id);
        preprocessData(eventData, "notification_wiki", wiki);
        preprocessData(eventData, "notification_type", type);
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void logMarkRead(@Nullable Long selectionToken) {
        log(
                "action_rank", 0,
                "selection_token", selectionToken
        );
    }

    public void logAction(int index, @NonNull Notification.Link link) {
        log(
                "action_rank", index + 1,
                "action_icon", link.getIcon()
        );
    }

    public void logClicked() {
        log(
                "action_rank", ACTION_CLICKED
        );
    }

    public void logDismissed() {
        log(
                "action_rank", ACTION_DISMISSED
        );
    }
}
