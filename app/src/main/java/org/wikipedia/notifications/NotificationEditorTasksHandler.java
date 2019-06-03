package org.wikipedia.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SuggestedEditsFunnel;
import org.wikipedia.dataclient.mwapi.EditorTaskCounts;
import org.wikipedia.events.CaptionEditUnlockEvent;
import org.wikipedia.events.DescriptionEditUnlockEvent;
import org.wikipedia.settings.Prefs;
import org.wikipedia.suggestededits.SuggestedEditsTasksActivity;

import static org.wikipedia.Constants.InvokeSource.NOTIFICATION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;

public final class NotificationEditorTasksHandler {

    public static void dispatchEditorTaskResults(@NonNull Context context, @NonNull EditorTaskCounts results) {
        Object eventToDispatch = null;

        int descriptionTargetsPassed = results.getDescriptionEditTargetsPassedCount();
        if (descriptionTargetsPassed > 1) {
            if (!Prefs.isSuggestedEditsAddDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification(context, false);
                eventToDispatch = new DescriptionEditUnlockEvent(1);
            }
            if (!Prefs.isSuggestedEditsTranslateDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateDescriptionsUnlocked(true);
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                    maybeShowTranslateDescriptionUnlockNotification(context, false);
                    eventToDispatch = new DescriptionEditUnlockEvent(descriptionTargetsPassed);
                }
            }
        } else if (descriptionTargetsPassed > 0) {
            if (!Prefs.isSuggestedEditsAddDescriptionsUnlocked()) {
                Prefs.setSuggestedEditsAddDescriptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditDescriptionUnlockNotification(context, false);
                eventToDispatch = new DescriptionEditUnlockEvent(descriptionTargetsPassed);
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

        int captionTargetsPassed = results.getCaptionEditTargetsPassedCount();
        if (captionTargetsPassed > 1) {
            if (!Prefs.isSuggestedEditsAddCaptionsUnlocked()) {
                Prefs.setSuggestedEditsAddCaptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditCaptionUnlockNotification(context, false);
                eventToDispatch = new CaptionEditUnlockEvent(1);
            }
            if (!Prefs.isSuggestedEditsTranslateCaptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateCaptionsUnlocked(true);
                if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                    maybeShowTranslateCaptionUnlockNotification(context, false);
                    eventToDispatch = new CaptionEditUnlockEvent(captionTargetsPassed);
                }
            }
        } else if (captionTargetsPassed > 0) {
            if (!Prefs.isSuggestedEditsAddCaptionsUnlocked()) {
                Prefs.setSuggestedEditsAddCaptionsUnlocked(true);
                Prefs.setShowActionFeedIndicator(true);
                Prefs.setShowEditMenuOptionIndicator(true);
                maybeShowEditCaptionUnlockNotification(context, false);
                eventToDispatch = new CaptionEditUnlockEvent(captionTargetsPassed);
            }
            if (Prefs.isSuggestedEditsTranslateCaptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateCaptionsUnlocked(false);
            }
        } else {
            if (Prefs.isSuggestedEditsAddCaptionsUnlocked()) {
                Prefs.setSuggestedEditsAddCaptionsUnlocked(false);
            }
            if (Prefs.isSuggestedEditsTranslateCaptionsUnlocked()) {
                Prefs.setSuggestedEditsTranslateCaptionsUnlocked(false);
            }
        }

        if (eventToDispatch != null) {
            WikipediaApp.getInstance().getBus().post(eventToDispatch);
        }
    }

    public static void maybeShowEditDescriptionUnlockNotification(@NonNull Context context, boolean forced) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed() || forced) {
            SuggestedEditsFunnel.get(NOTIFICATION).pause();
            Intent intent = SuggestedEditsTasksActivity.newIntent(context, SUGGESTED_EDITS_ADD_DESC);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_title),
                    context.getString(R.string.suggested_edits_unlock_notification_text),
                    context.getString(R.string.suggested_edits_unlock_add_descriptions_notification_big_text),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    public static void maybeShowTranslateDescriptionUnlockNotification(@NonNull Context context, boolean forced) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed() || forced) {
            SuggestedEditsFunnel.get(NOTIFICATION).pause();
            Intent intent = SuggestedEditsTasksActivity.newIntent(context, SUGGESTED_EDITS_TRANSLATE_DESC);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_title),
                    context.getString(R.string.suggested_edits_unlock_notification_text),
                    context.getString(R.string.suggested_edits_unlock_translate_descriptions_notification_big_text),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    public static void maybeShowEditCaptionUnlockNotification(@NonNull Context context, boolean forced) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed() || forced) {
            SuggestedEditsFunnel.get(NOTIFICATION).pause();
            Intent intent = SuggestedEditsTasksActivity.newIntent(context, SUGGESTED_EDITS_ADD_CAPTION);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_unlock_add_captions_notification_title),
                    context.getString(R.string.suggested_edits_unlock_notification_text),
                    context.getString(R.string.suggested_edits_unlock_add_captions_notification_big_text),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    public static void maybeShowTranslateCaptionUnlockNotification(@NonNull Context context, boolean forced) {
        if (!WikipediaApp.getInstance().isAnyActivityResumed() || forced) {
            SuggestedEditsFunnel.get(NOTIFICATION).pause();
            Intent intent = SuggestedEditsTasksActivity.newIntent(context, SUGGESTED_EDITS_TRANSLATE_DESC);
            NotificationCompat.Builder builder = NotificationPresenter.getDefaultBuilder(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, context.getString(R.string.suggested_edits_unlock_notification_button), pendingIntent);
            NotificationPresenter.showNotification(context, builder, 0, context.getString(R.string.suggested_edits_unlock_translate_captions_notification_title),
                    context.getString(R.string.suggested_edits_unlock_notification_text),
                    context.getString(R.string.suggested_edits_unlock_translate_captions_notification_big_text),
                    R.drawable.ic_mode_edit_white_24dp, R.color.accent50, intent);
        }
    }

    private NotificationEditorTasksHandler() {
    }
}
