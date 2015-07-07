package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.wikipedia.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        PreferenceHostCompat prefHost = new PreferenceHostCompat(this);
        SettingsUI ui = new SettingsUI(getActivity(), prefHost);
        ui.loadPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
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

    private void launchDeveloperSettingsActivity() {
        startActivity(DeveloperSettingsActivity.newIntent(getActivity()));
    }

    private void prepareDeveloperSettingsMenuItem(Menu menu) {
        menu.findItem(R.id.developer_settings).setVisible(Prefs.isShowDeveloperSettingsEnabled());
    }

    private void invalidateOptionsMenu() {
        getFragmentManager().invalidateOptionsMenu();
    }
}
