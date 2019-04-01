package org.wikipedia.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SuggestedEditsFunnel;
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
            if (!Prefs.isEditActionAddDescriptionsUnlocked()) {
                Prefs.setEditActionAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification(context);
                eventToDispatch = new EditorTaskUnlockEvent(1);
            }
            if (!Prefs.isEditActionTranslateDescriptionsUnlocked() && WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(true);
                maybeShowTranslateDescriptionUnlockNotification(context);
                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
        } else if (numTargetsPassed > 0) {
            if (!Prefs.isEditActionAddDescriptionsUnlocked()) {
                Prefs.setEditActionAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification(context);
                eventToDispatch = new EditorTaskUnlockEvent(numTargetsPassed);
            }
            if (Prefs.isEditActionTranslateDescriptionsUnlocked()) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(false);
            }
        } else {
            if (Prefs.isEditActionAddDescriptionsUnlocked()) {
                Prefs.setEditActionAddDescriptionsUnlocked(false);
            }
            if (Prefs.isEditActionTranslateDescriptionsUnlocked()) {
                Prefs.setEditActionTranslateDescriptionsUnlocked(false);
            }
        }

        if (eventToDispatch != null) {
            WikipediaApp.getInstance().getBus().post(eventToDispatch);
        }
    }

    private static void maybeShowEditDescriptionUnlockNotification(@NonNull Context context) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
            SuggestedEditsFunnel.get(Constants.InvokeSource.NOTIFICATION).pause();
            Intent intent = AddTitleDescriptionsActivity.Companion.newIntent(context, Constants.InvokeSource.EDIT_FEED_TITLE_DESC);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.title_description_get_started), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.description_edit_task_unlock_title),
                    context.getString(R.string.description_edit_task_unlock_body),
                    context.getString(R.string.description_edit_task_unlock_body),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    private static void maybeShowTranslateDescriptionUnlockNotification(@NonNull Context context) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed()) {
            SuggestedEditsFunnel.get(Constants.InvokeSource.NOTIFICATION).pause();
            Intent intent = AddTitleDescriptionsActivity.Companion.newIntent(context, Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.title_description_get_started), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.translation_description_edit_task_unlock_title),
                    context.getString(R.string.translation_description_edit_task_unlock_body),
                    context.getString(R.string.translation_description_edit_task_unlock_body),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    private NotificationEditorTasksHandler() {
    }
}
