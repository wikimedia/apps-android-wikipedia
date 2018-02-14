package org.wikipedia.readinglist;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.wikipedia.R;
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
                (@NonNull String url, @Nullable String notUsed) -> {
                    FeedbackUtil.showAndroidAppFAQ(activity);
                }));
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
