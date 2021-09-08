package org.wikipedia.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceFragmentCompat
import org.wikipedia.R

internal class NotificationSettingsPreferenceLoader(fragment: PreferenceFragmentCompat) : BasePreferenceLoader(fragment) {
    override fun loadPreferences() {
        loadPreferences(R.xml.preferences_notifications)
        (findPreference(R.string.preference_key_notification_customize_push) as PreferenceMultiLine).setOnPreferenceClickListener {
            activity.startActivity(openNotificationSettings())
            true
        }
    }

    private fun openNotificationSettings(): Intent {
        return Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", activity.packageName)
                    putExtra("app_uid", activity.applicationInfo.uid)
                }
            }
        }
    }
}
