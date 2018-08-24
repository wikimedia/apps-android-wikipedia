package org.wikipedia.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.NotificationPreferencesFunnel;

public class NotificationSettingsActivity extends SingleFragmentActivity<NotificationSettingsFragment> {
    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, NotificationSettingsActivity.class);
    }

    @Override
    public NotificationSettingsFragment createFragment() {
        return NotificationSettingsFragment.newInstance();
    }

    @Override protected void onDestroy() {
        new NotificationPreferencesFunnel(WikipediaApp.getInstance()).done();
        super.onDestroy();
    }

    public static void promptEnablePollDialog(@NonNull Activity activity) {
        if (!Prefs.notificationPollReminderEnabled() || Prefs.notificationPollEnabled()) {
            return;
        }
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_with_checkbox, null);
        TextView message = view.findViewById(R.id.dialog_message);
        CheckBox checkbox = view.findViewById(R.id.dialog_checkbox);
        message.setText(activity.getString(R.string.preference_summary_notification_poll));
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.notifications_poll_enable_title)
                .setView(view)
                .setPositiveButton(R.string.notifications_poll_enable_positive,
                        (dialogInterface, i) -> {
                            Prefs.setNotificationPollEnabled(true);
                        })
                .setNegativeButton(R.string.notifications_poll_enable_negative, null)
                .setOnDismissListener((dialog) -> {
                    Prefs.setNotificationPollReminderEnabled(!checkbox.isChecked());
                })
                .show();
    }
}
