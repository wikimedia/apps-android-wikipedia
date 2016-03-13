package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

/*package*/ abstract class PreferenceLoaderFragment extends PreferenceFragmentCompat
        implements PreferenceLoader {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
    }
}