package org.wikipedia.settings;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

@TargetApi(11)
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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
                getActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LOGOUT);
                getActivity().finish();
                return false;
            }
        });

        if (!BuildConfig.APPLICATION_ID.equals("org.wikipedia")) {
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
                intent.setClass(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefKeys.getContentLanguageKey())) {
            LanguagePreference pref = (LanguagePreference) findPreference(PrefKeys.getContentLanguageKey());
            pref.setSummary(WikipediaApp.getInstance().getDisplayLanguage());
            getActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
        }
    }
}
