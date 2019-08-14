package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.notifications.Notification;
import org.wikipedia.settings.Prefs;

import java.util.HashMap;
import java.util.Map;

public class NotificationPreferencesFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppNotificationPreferences";
    private static final int REV_ID = 18325724;

    public NotificationPreferencesFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REV_ID);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void done() {
        Map<String, Boolean> toggleMap = new HashMap<>();
        toggleMap.put(Notification.CATEGORY_SYSTEM_NO_EMAIL, Prefs.notificationWelcomeEnabled());
        toggleMap.put(Notification.CATEGORY_EDIT_THANK, Prefs.notificationThanksEnabled());
        toggleMap.put(Notification.CATEGORY_THANK_YOU_EDIT, Prefs.notificationMilestoneEnabled());

        log(
                "type_toggles", GsonMarshaller.marshal(toggleMap),
                "background_fetch", Prefs.notificationPollEnabled() ? Integer.toString(getApp().getResources().getInteger(R.integer.notification_poll_interval_minutes)) : "disabled"
        );
    }
}
