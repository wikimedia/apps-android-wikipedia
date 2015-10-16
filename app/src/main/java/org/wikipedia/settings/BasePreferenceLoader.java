package org.wikipedia.settings;

import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;

/*package*/ abstract class BasePreferenceLoader implements PreferenceLoader {
    @NonNull private final PreferenceFragment preferenceHost;

    /*package*/ BasePreferenceLoader(@NonNull PreferenceFragment fragment) {
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
