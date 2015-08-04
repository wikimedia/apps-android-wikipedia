package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DeveloperSettingsFragment extends PreferenceLoaderFragment {
    public static DeveloperSettingsFragment newInstance() {
        return new DeveloperSettingsFragment();
    }

    @Override
    public void loadPreferences() {
        PreferenceLoader preferenceLoader = new DeveloperSettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }
}