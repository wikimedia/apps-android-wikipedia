package org.wikipedia.settings;

import org.wikipedia.R;
import org.wikipedia.server.mwapi.MwPageEndpointsCache;
import org.wikipedia.server.restbase.RbPageEndpointsCache;

import android.content.SharedPreferences;

public class DeveloperSettingsFragment extends PreferenceLoaderFragment {
    public static DeveloperSettingsFragment newInstance() {
        return new DeveloperSettingsFragment();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener onChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getString(R.string.preference_key_restbase_uri_format).equals(key)) {
                RbPageEndpointsCache.INSTANCE.update();
            } else if (getString(R.string.preference_key_mediawiki_base_uri).equals(key)) {
                MwPageEndpointsCache.INSTANCE.update();
            }
        }
    };

    @Override
    public void loadPreferences() {
        PreferenceLoader preferenceLoader = new DeveloperSettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(onChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(onChangeListener);
    }
}