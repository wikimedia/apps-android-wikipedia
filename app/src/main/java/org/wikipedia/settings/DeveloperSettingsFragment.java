package org.wikipedia.settings;

import android.os.Bundle;

public class DeveloperSettingsFragment extends PreferenceLoaderFragment {
    public static DeveloperSettingsFragment newInstance() {
        return new DeveloperSettingsFragment();
    }

    @Override
    public void loadPreferences() {
        PreferenceLoader preferenceLoader = new DeveloperSettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }
}