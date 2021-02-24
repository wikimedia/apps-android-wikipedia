package org.wikipedia.settings

import android.app.Activity
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

internal abstract class BasePreferenceLoader(private val preferenceHost: PreferenceFragmentCompat) : PreferenceLoader {
    fun findPreference(@StringRes key: Int): Preference {
        return findPreference(getKey(key))
    }

    private fun findPreference(key: CharSequence?): Preference {
        return preferenceHost.findPreference(key!!)!!
    }

    protected fun loadPreferences(@XmlRes id: Int) {
        preferenceHost.addPreferencesFromResource(id)
    }

    private fun getKey(@StringRes id: Int): String {
        return activity.getString(id)
    }

    protected val activity: Activity
        get() = preferenceHost.requireActivity()
}
