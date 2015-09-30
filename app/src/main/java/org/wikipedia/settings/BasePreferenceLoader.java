package org.wikipedia.settings;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;

/*package*/ abstract class BasePreferenceLoader implements PreferenceLoader {
    @NonNull private final PreferenceFragment preferenceHost;

    /*package*/ BasePreferenceLoader(@NonNull PreferenceFragment fragment) {
        preferenceHost = fragment;
    }

    protected Preference findPreference(CharSequence key) {
        return preferenceHost.findPreference(key);
    }

    protected void loadPreferences(@XmlRes int id) {
        preferenceHost.addPreferencesFromResource(id);
    }
}