package org.wikipedia.settings

class NotificationSettingsFragment : PreferenceLoaderFragment() {
    override fun loadPreferences() {
        NotificationSettingsPreferenceLoader(this).loadPreferences()
    }

    companion object {
        fun newInstance(): NotificationSettingsFragment {
            return NotificationSettingsFragment()
        }
    }
}
