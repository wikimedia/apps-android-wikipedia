package org.wikipedia.settings;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import org.wikipedia.*;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        // Hmm. Can't use ActionBarActivity?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
        }
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
        }

    }
}