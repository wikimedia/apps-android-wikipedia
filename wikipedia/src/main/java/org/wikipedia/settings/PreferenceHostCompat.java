package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

/**
 * An abstraction layer over PreferenceActivity and PreferenceFragment.
 * This is unfortunately needed since, as of this writing, there is no support-v4 version
 * of PreferenceFragment. The correct implementation is used based on the parameter passed to
 * the constructor.
 * Note: this only implements the methods this app is using.
 */
public class PreferenceHostCompat implements PreferenceHost {
    private final PreferenceHost impl;

    /** For Gingerbread (and lower) */
    public PreferenceHostCompat(PreferenceActivity preferenceActivity) {
        impl = new PreferenceHostCompatGB(preferenceActivity);
    }

    /** For Honeycomb and higher */
    public PreferenceHostCompat(PreferenceFragment preferenceFragment) {
        impl = new PreferenceHostCompatHC(preferenceFragment);
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        impl.addPreferencesFromResource(preferencesResId);
    }

    @Override
    public Preference findPreference(CharSequence key) {
        return impl.findPreference(key);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class PreferenceHostCompatHC implements PreferenceHost {
        private final PreferenceFragment host;

        public PreferenceHostCompatHC(PreferenceFragment preferenceFragment) {
            host = preferenceFragment;
        }

        @Override
        public void addPreferencesFromResource(int preferencesResId) {
            host.addPreferencesFromResource(preferencesResId);
        }

        @Override
        public Preference findPreference(CharSequence key) {
            return host.findPreference(key);
        }
    }

    @SuppressWarnings("deprecation")
    private class PreferenceHostCompatGB implements PreferenceHost {
        private final PreferenceActivity host;

        public PreferenceHostCompatGB(PreferenceActivity preferenceActivity) {
            host = preferenceActivity;
        }

        @Override
        public void addPreferencesFromResource(int preferencesResId) {
            host.addPreferencesFromResource(preferencesResId);
        }

        @Override
        public Preference findPreference(CharSequence key) {
            return host.findPreference(key);
        }
    }
}

interface PreferenceHost {
    void addPreferencesFromResource(int preferencesResId);
    Preference findPreference(CharSequence key);
}
