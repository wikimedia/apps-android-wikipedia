package org.wikipedia.readinglist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;

public final class ReadingListSyncBehaviorDialogs {

    private static boolean SHOWING_DIALOG;

    public interface Callback {
        void onDialogDismiss();
    }


    public static void detectedMultipleSyncSettingsDialog(Context context, Callback callback) {
        if (!SHOWING_DIALOG) {
            SHOWING_DIALOG = true;
            new AlertDialog.Builder(context)
                    .setTitle(R.string.reading_list_turned_sync_off_dialog_title)
                    .setMessage(R.string.reading_list_turned_sync_off_dialog_text)
                    .setPositiveButton(R.string.reading_list_turned_sync_off_dialog_ok,
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .setNegativeButton(R.string.reading_list_turned_sync_off_dialog_settings,
                            (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                context.startActivity(SettingsActivity.newIntent(context));
                            })
                    .setOnDismissListener((dialog) -> {
                        SHOWING_DIALOG = false;
                        callback.onDialogDismiss();
                    })
                    .show();
        }
    }

    public static void promptTurnSyncOnDialog(Context context, LayoutInflater inflater, boolean gotoSettingsActivity, Callback callback) {
        if (!SHOWING_DIALOG) {
            View view = inflater.inflate(R.layout.dialog_with_checkbox, null);
            TextView message = view.findViewById(R.id.dialog_message);
            CheckBox checkbox = view.findViewById(R.id.dialog_checkbox);
            message.setText(StringUtil.fromHtml(context.getString(R.string.reading_list_prompt_turned_sync_on_dialog_text)));
            message.setMovementMethod(new LinkMovementMethodExt(
                    (@NonNull String url, @Nullable String notUsed) -> {
                        FeedbackUtil.showAndroidAppFAQ(context);
                    }));
            new AlertDialog.Builder(context)
                    .setTitle(R.string.reading_list_prompt_turned_sync_on_dialog_title)
                    .setView(view)
                    .setPositiveButton(R.string.reading_list_prompt_turned_sync_on_dialog_enable_syncing,
                            (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                if (gotoSettingsActivity) {
                                    context.startActivity(SettingsActivity.newIntent(context));
                                }
                            })
                    .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks,
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .setOnDismissListener((dialog) -> {
                        Prefs.setShowDialogPromptOptInSyncReadingListsEnabled(!checkbox.isChecked());
                        SHOWING_DIALOG = false;
                        callback.onDialogDismiss();
                    })
                    .show();
        }
    }

    public static void removeExistListsDialog(Context context, Callback callback) {
        if (!SHOWING_DIALOG) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.reading_list_logout_option_reminder_dialog_title)
                    .setMessage(R.string.reading_list_logout_option_reminder_dialog_text)
                    .setPositiveButton(R.string.reading_list_logout_option_reminder_dialog_yes,
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .setNegativeButton(R.string.reading_list_logout_option_reminder_dialog_no,
                            (dialogInterface, i) -> {
                                ReadingListDbHelper.instance().resetToDefaults();
                                dialogInterface.dismiss();
                            })
                    .setOnDismissListener((dialog) -> {
                        SHOWING_DIALOG = false;
                        callback.onDialogDismiss();
                    })
                    .show();
        }
    }

    public static void mergeAndSyncDialog(Context context, Callback callback) {
        if (!SHOWING_DIALOG) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.reading_list_login_option_reminder_dialog_title)
                    .setMessage(R.string.reading_list_login_option_reminder_dialog_text)
                    .setPositiveButton(R.string.reading_list_login_option_reminder_dialog_yes,
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .setNegativeButton(R.string.reading_list_login_option_reminder_dialog_no,
                            (dialogInterface, i) -> {
                                // should finish the remove & create process before doing the manual sync,
                                // otherwise the lists will be removed from sync server
                                ReadingListDbHelper.instance().resetToDefaults();
                                Prefs.setReadingListsLastSyncTime(null);
                                dialogInterface.dismiss();
                            })
                    .setOnDismissListener((dialog) -> {
                        SHOWING_DIALOG = false;
                        callback.onDialogDismiss();
                    })
                    .show();
        }
    }

    private ReadingListSyncBehaviorDialogs() {
    }
}
