package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

abstract class PreferenceLoaderFragment extends PreferenceFragmentCompat
        implements PreferenceLoader {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        loadPreferences();
    }
}
