package org.wikipedia.notifications;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.EditorTaskCounts;
import org.wikipedia.events.EditorTaskUnlockEvent;
import org.wikipedia.settings.Prefs;

import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;

public final class NotificationEditorTasksHandler {

    public static void dispatchEditorTaskResults(@NonNull EditorTaskCounts results) {
        int numTargetsPassed = results.getDescriptionEditTargetsPassed().size();
        Object eventToDispatch = null;

        if (numTargetsPassed > 1) {
            if (!Prefs.isActionEditDescriptionsUnlocked()) {
                Prefs.setActionEditDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification();
                eventToDispatch = new EditorTaskUnlockEvent(1);
            }
            if (!Prefs.isEditActionTranslateDescriptionsUnlocked() && WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(true);
                maybeShowTranslateDescriptionUnlockNotification();
                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
        } else if (numTargetsPassed > 0) {
            if (!Prefs.isActionEditDescriptionsUnlocked()) {
                Prefs.setActionEditDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification();
                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
            if (Prefs.isEditActionTranslateDescriptionsUnlocked()) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(false);
            }
        } else {
            if (Prefs.isActionEditDescriptionsUnlocked()) {
                Prefs.setActionEditDescriptionsUnlocked(false);
            }
            if (Prefs.isEditActionTranslateDescriptionsUnlocked()) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(false);
            }
        }

        if (eventToDispatch != null) {
            WikipediaApp.getInstance().getBus().post(eventToDispatch);
        }
    }

    private static void maybeShowEditDescriptionUnlockNotification() {
        if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
            // TODO: show the notification!
        }
    }

    private static void maybeShowTranslateDescriptionUnlockNotification() {
        if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
            // TODO: show the notification!
        }
    }

    private NotificationEditorTasksHandler() {
    }
}
