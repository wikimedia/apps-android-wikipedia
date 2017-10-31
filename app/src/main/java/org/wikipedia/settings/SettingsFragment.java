package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.wikipedia.R;

public class SettingsFragment extends PreferenceLoaderFragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private SettingsPreferenceLoader preferenceLoader;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void loadPreferences() {
        preferenceLoader = new SettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        prepareDeveloperSettingsMenuItem(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.developer_settings:
                launchDeveloperSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateOfflineLibraryPref(boolean checked) {
        ((SwitchPreferenceCompat) preferenceLoader.findPreference(R.string.preference_key_enable_offline_library))
                .setChecked(checked);
    }

    private void launchDeveloperSettingsActivity() {
        startActivity(DeveloperSettingsActivity.newIntent(getActivity()));
    }

    private void prepareDeveloperSettingsMenuItem(Menu menu) {
        menu.findItem(R.id.developer_settings).setVisible(Prefs.isShowDeveloperSettingsEnabled());
    }
}
