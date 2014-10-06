package org.wikipedia.settings;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivityWithBack implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;
    public static final int ACTIVITY_RESULT_LOGOUT = 2;
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference logoutPref = findPreference(getString(R.string.preference_key_logout));
        if (!WikipediaApp.getInstance().getUserInfoStorage().isLoggedIn()) {
            logoutPref.setEnabled(false);
            logoutPref.setSummary(getString(R.string.preference_summary_notloggedin));
        }
        logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setResult(ACTIVITY_RESULT_LOGOUT);
                finish();
                return false;
            }
        });

        if (!BuildConfig.PACKAGE_NAME.equals("org.wikipedia")) {
            overridePackageName();
        }
    }

    /**
     * Needed for beta release since the Gradle flavors applicationId changes don't get reflected
     * to the preferences.xml
     * See https://code.google.com/p/android/issues/detail?id=57460
     */
    private void overridePackageName() {
        Preference aboutPref = findPreference("about");
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefKeys.getContentLanguageKey())) {
            LanguagePreference pref = (LanguagePreference) findPreference(PrefKeys.getContentLanguageKey());
            pref.setSummary(pref.getCurrentLanguageDisplayString());
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED);
        }
    }
}
