package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.util.ApiUtil;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceLoaderFragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void loadPreferences() {
        SettingsPreferenceLoader preferenceLoader = new SettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
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

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void invalidateOptionsMenu() {
        if (ApiUtil.hasIceCreamSandwich()) {
            getFragmentManager().invalidateOptionsMenu();
        } else {
            getActivity().invalidateOptionsMenu();
        }
    }
}
