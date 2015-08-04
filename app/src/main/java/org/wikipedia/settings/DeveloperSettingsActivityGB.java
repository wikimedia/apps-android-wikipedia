package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;

public class DeveloperSettingsActivityGB extends LegacyPreferenceActivity {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivityGB.class);
    }

    @Override
    public void loadPreferences() {
        PreferenceLoader preferenceLoader = new DeveloperSettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }
}