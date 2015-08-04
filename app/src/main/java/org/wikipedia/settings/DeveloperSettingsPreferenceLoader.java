package org.wikipedia.settings;

import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import org.wikipedia.R;

/*package*/ class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    /*package*/ DeveloperSettingsPreferenceLoader(@NonNull PreferenceActivity activity) {
        super(activity);
    }

    /*package*/ DeveloperSettingsPreferenceLoader(@NonNull PreferenceFragment fragment) {
        super(fragment);
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.developer_preferences);
    }
}