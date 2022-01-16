package org.wikipedia.settings

import android.app.Activity
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

internal abstract class BasePreferenceLoader(private val preferenceHost: PreferenceFragmentCompat) : PreferenceLoader {
    fun findPreference(@StringRes key: Int): Preference {
        return preferenceHost.findPreference(activity.getString(key))!!
    }

    protected fun loadPreferences(@XmlRes id: Int) {
        preferenceHost.addPreferencesFromResource(id)
    }

    protected val activity: Activity
        get() = preferenceHost.requireActivity()
}
