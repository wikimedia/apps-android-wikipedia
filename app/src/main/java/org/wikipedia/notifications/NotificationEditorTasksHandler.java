package org.wikipedia.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.EditorTaskCounts;
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity;
import org.wikipedia.events.EditorTaskUnlockEvent;
import org.wikipedia.settings.Prefs;

import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;

public final class NotificationEditorTasksHandler {

    public static void dispatchEditorTaskResults(@NonNull Context context, @NonNull EditorTaskCounts results) {
        int numTargetsPassed = results.getDescriptionEditTargetsPassed().size();
        Object eventToDispatch = null;

        if (numTargetsPassed > 1) {
            if (!Prefs.isSuggestedEditsAddDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);

                if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
                    maybeShowEditDescriptionUnlockNotification(context);
                }

                eventToDispatch = new EditorTaskUnlockEvent(1);
            }
            if (!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked() && WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                Prefs.setSuggestedEditsTranslateDescriptionsUnlocked(true);

                if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
                    maybeShowTranslateDescriptionUnlockNotification(context);
                }

                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
        } else if (numTargetsPassed > 0) {
            if (!Prefs.isSuggestedEditsAddDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);

                if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
                    maybeShowEditDescriptionUnlockNotification(context);
                }

                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
            if (Prefs.isSuggestedEditsTranslateDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateDescriptionsUnlocked(false);
            }
        } else {
            if (Prefs.isSuggestedEditsAddDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsAddDescriptionsUnlocked(false);
            }
            if (Prefs.isSuggestedEditsTranslateDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateDescriptionsUnlocked(false);
            }
        }

        if (eventToDispatch != null) {
            WikipediaApp.getInstance().getBus().post(eventToDispatch);
        }
    }

    public static void maybeShowEditDescriptionUnlockNotification(@NonNull Context context) {
        Intent intent = AddTitleDescriptionsActivity.Companion.newIntent(context, Constants.InvokeSource.EDIT_FEED_TITLE_DESC);
        NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
        NotificationPresenter.showNotification(context, 0, context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_title),
                context.getString(R.string.suggested_edits_unlock_notification_text),
                context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_big_text),
                R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent, builder);
    }

    public static void maybeShowTranslateDescriptionUnlockNotification(@NonNull Context context) {
        Intent intent = AddTitleDescriptionsActivity.Companion.newIntent(context, Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC);
        NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
        NotificationPresenter.showNotification(context, 0, context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_title),
                context.getString(R.string.suggested_edits_unlock_notification_text),
                context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_big_text),
                R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent, builder);
    }

    private NotificationEditorTasksHandler() {
    }
}
