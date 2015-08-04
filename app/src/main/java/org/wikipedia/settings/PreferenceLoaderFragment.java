package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
/*package*/ abstract class PreferenceLoaderFragment extends PreferenceFragment
        implements PreferenceLoader {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
    }
}