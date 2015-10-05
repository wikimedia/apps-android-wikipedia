package org.wikipedia.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/*package*/ abstract class PreferenceLoaderFragment extends PreferenceFragment
        implements PreferenceLoader {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
    }
}