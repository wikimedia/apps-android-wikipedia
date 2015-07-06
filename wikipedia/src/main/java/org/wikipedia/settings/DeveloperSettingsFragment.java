package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.wikipedia.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DeveloperSettingsFragment extends PreferenceFragment {
    public static DeveloperSettingsFragment newInstance() {
        return new DeveloperSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
    }

    private void loadPreferences() {
        addPreferencesFromResource(R.xml.developer_preferences);
    }
}