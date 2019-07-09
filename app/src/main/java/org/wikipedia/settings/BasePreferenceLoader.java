package org.wikipedia.settings;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/*package*/ abstract class BasePreferenceLoader implements PreferenceLoader {
    @NonNull private final PreferenceFragmentCompat preferenceHost;

    /*package*/ BasePreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        preferenceHost = fragment;
    }

    protected PreferenceFragmentCompat getPreferenceHost() {
        return preferenceHost;
    }

    protected Preference findPreference(@StringRes int key) {
        return findPreference(getKey(key));
    }

    protected Preference findPreference(CharSequence key) {
        return preferenceHost.findPreference(key);
    }

    protected void loadPreferences(@XmlRes int id) {
        preferenceHost.addPreferencesFromResource(id);
    }

    private String getKey(@StringRes int id) {
        return getActivity().getString(id);
    }

    protected Activity getActivity() {
        return preferenceHost.getActivity();
    }
}
