package org.wikipedia.settings

import android.app.Activity
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

internal abstract class BasePreferenceLoader(protected val fragment: PreferenceFragmentCompat) : PreferenceLoader {
    fun findPreference(@StringRes key: Int): Preference {
        return fragment.findPreference((activity.getString((key))))!!
    }

    protected fun loadPreferences(@XmlRes id: Int) {
        fragment.addPreferencesFromResource(id)
    }

    protected val activity: Activity
        get() = fragment.requireActivity()
}
