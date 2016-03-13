package org.wikipedia.settings;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

/*package*/ abstract class BasePreferenceLoader implements PreferenceLoader {
    @NonNull private final PreferenceFragmentCompat preferenceHost;

    /*package*/ BasePreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        preferenceHost = fragment;
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
