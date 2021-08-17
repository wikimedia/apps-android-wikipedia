package org.wikipedia.settings

import androidx.preference.PreferenceFragmentCompat
import org.wikipedia.R

internal class NotificationSettingsPreferenceLoader(fragment: PreferenceFragmentCompat) : BasePreferenceLoader(fragment) {
    override fun loadPreferences() {
        loadPreferences(R.xml.preferences_notifications)
    }
}
