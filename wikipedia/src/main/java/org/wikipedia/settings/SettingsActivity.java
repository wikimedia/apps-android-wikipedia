package org.wikipedia.settings;

import android.content.*;
import android.os.*;
import android.preference.*;
import org.wikipedia.*;

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
        if (key.equals(WikipediaApp.PREFERENCE_CONTENT_LANGUAGE)) {
            LanguagePreference pref = (LanguagePreference) findPreference(WikipediaApp.PREFERENCE_CONTENT_LANGUAGE);
            pref.setSummary(pref.getCurrentLanguageDisplayString());
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED);
        }
    }
}