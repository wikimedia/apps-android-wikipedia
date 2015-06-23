package org.wikipedia.settings;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.MenuItem;

/**
 * Settings activity that is specifically intended for API 10.
 * It's functionally identical to the real SettingsActivity, except that this one inherits from
 * PreferenceActivity, which was deprecated after API 10. The new SettingsActivity inherits from
 * ActionBarActivity, and uses a PreferenceFragment, all of which are necessary for all the
 * components to render properly (specifically checkboxes).
 */
public class SettingsActivityGB extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
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
                setResult(SettingsActivity.ACTIVITY_RESULT_LOGOUT);
                finish();
                return false;
            }
        });

        if (!BuildConfig.APPLICATION_ID.equals("org.wikipedia")) {
            overridePackageName();
        }
    }

    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
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
                intent.setClass(SettingsActivityGB.this, AboutActivity.class);
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
        String languageKey = Prefs.getAppLanguageCodeKey();
        if (key.equals(languageKey)) {
            LanguagePreference pref = (LanguagePreference) findPreference(languageKey);
            pref.setSummary(WikipediaApp.getInstance().getAppLanguageLocalizedName());
            setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
        }
    }
}
