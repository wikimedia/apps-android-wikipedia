package org.wikipedia.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.NotificationPreferencesFunnel

class NotificationSettingsActivity : SingleFragmentActivity<NotificationSettingsFragment>() {
    public override fun createFragment(): NotificationSettingsFragment {
        return NotificationSettingsFragment.newInstance()
    }

    override fun onDestroy() {
        NotificationPreferencesFunnel(WikipediaApp.getInstance()).done()
        super.onDestroy()
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, NotificationSettingsActivity::class.java)
        }

        fun promptEnablePollDialog(activity: Activity) {
            if (!Prefs.isNotificationPollReminderEnabled || Prefs.isNotificationPollEnabled) {
                return
            }
            val view = activity.layoutInflater.inflate(R.layout.dialog_with_checkbox, null)
            val message = view.findViewById<TextView>(R.id.dialog_message)
            val checkbox = view.findViewById<CheckBox>(R.id.dialog_checkbox)
            message.text = activity.getString(R.string.preference_summary_notification_poll)
            AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(R.string.notifications_poll_enable_title)
                    .setView(view)
                    .setPositiveButton(R.string.notifications_poll_enable_positive) { _, _ ->
                        Prefs.isNotificationPollEnabled = true
                    }
                    .setNegativeButton(R.string.notifications_poll_enable_negative, null)
                    .setOnDismissListener {
                        Prefs.isNotificationPollReminderEnabled = !checkbox.isChecked
                    }
                    .show()
        }
    }
}
