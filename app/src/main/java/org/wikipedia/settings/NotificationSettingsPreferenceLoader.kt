package org.wikipedia.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.os.bundleOf
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
                    putExtras(bundleOf("app_package" to activity.packageName,
                        "app_uid" to activity.applicationInfo.uid))
                }
            }
        }
    }
}
