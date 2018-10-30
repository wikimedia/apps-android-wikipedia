package org.wikipedia.readinglist;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;

public final class ReadingListSyncBehaviorDialogs {

    public static void detectedRemoteTornDownDialog(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_turned_sync_off_dialog_title)
                .setMessage(R.string.reading_list_turned_sync_off_dialog_text)
                .setPositiveButton(R.string.reading_list_turned_sync_off_dialog_ok, null)
                .setNegativeButton(R.string.reading_list_turned_sync_off_dialog_settings,
                        (dialogInterface, i) -> {
                            activity.startActivity(SettingsActivity.newIntent(activity));
                        })
                .show();
    }

    public static void promptEnableSyncDialog(@NonNull Activity activity) {
        if (!Prefs.shouldShowReadingListSyncEnablePrompt()) {
            return;
        }
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_with_checkbox, null);
        TextView message = view.findViewById(R.id.dialog_message);
        CheckBox checkbox = view.findViewById(R.id.dialog_checkbox);
        message.setText(StringUtil.fromHtml(activity.getString(R.string.reading_list_prompt_turned_sync_on_dialog_text)));
        message.setMovementMethod(new LinkMovementMethodExt(
                (@NonNull String url) -> FeedbackUtil.showAndroidAppFAQ(activity)));
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_prompt_turned_sync_on_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.reading_list_prompt_turned_sync_on_dialog_enable_syncing,
                        (dialogInterface, i) -> {
                            Prefs.shouldShowReadingListSyncMergePrompt(true);
                            ReadingListSyncAdapter.setSyncEnabledWithSetup();
                        })
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks, null)
                .setOnDismissListener((dialog) -> {
                    Prefs.shouldShowReadingListSyncEnablePrompt(!checkbox.isChecked());
                    WikipediaApp.getInstance().getBus().post(new ReadingListsEnableSyncStatusEvent());
                })
                .show();
    }

    static void promptLogInToSyncDialog(@NonNull Activity activity) {
        if (!Prefs.shouldShowReadingListSyncEnablePrompt()) {
            return;
        }
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_with_checkbox, null);
        TextView message = view.findViewById(R.id.dialog_message);
        CheckBox checkbox = view.findViewById(R.id.dialog_checkbox);
        message.setText(StringUtil.fromHtml(activity.getString(R.string.reading_lists_login_reminder_text_with_link)));
        message.setMovementMethod(new LinkMovementMethodExt(
                (@NonNull String url) -> FeedbackUtil.showAndroidAppFAQ(activity)));
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_login_reminder_title)
                .setView(view)
                .setPositiveButton(R.string.reading_list_preference_login_or_signup_to_enable_sync_dialog_login,
                        (dialogInterface, i) -> {
                            Intent loginIntent = LoginActivity.newIntent(activity,
                                    LoginFunnel.SOURCE_READING_MANUAL_SYNC);

                            activity.startActivity(loginIntent);
                        })
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks, null)
                .setOnDismissListener((dialog) -> {
                    Prefs.shouldShowReadingListSyncEnablePrompt(!checkbox.isChecked());
                })
                .show();
    }

    public static void removeExistingListsOnLogoutDialog(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_logout_option_reminder_dialog_title)
                .setMessage(R.string.reading_list_logout_option_reminder_dialog_text)
                .setPositiveButton(R.string.reading_list_logout_option_reminder_dialog_yes, null)
                .setNegativeButton(R.string.reading_list_logout_option_reminder_dialog_no,
                        (dialogInterface, i) -> {
                            ReadingListDbHelper.instance().resetToDefaults();
                            SavedPageSyncService.sendSyncEvent();
                        })
                .show();
    }

    public static void mergeExistingListsOnLoginDialog(@NonNull Activity activity) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_login_option_reminder_dialog_title)
                .setMessage(R.string.reading_list_login_option_reminder_dialog_text)
                .setPositiveButton(R.string.reading_list_login_option_reminder_dialog_yes, null)
                .setNegativeButton(R.string.reading_list_login_option_reminder_dialog_no,
                        (dialogInterface, i) -> {
                            ReadingListDbHelper.instance().resetToDefaults();
                            SavedPageSyncService.sendSyncEvent();
                            Prefs.setReadingListsLastSyncTime(null);
                        })
                .setOnDismissListener(dialog -> ReadingListSyncAdapter.manualSyncWithForce())
                .show();
    }

    private ReadingListSyncBehaviorDialogs() {
    }
}
