package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.ReadingListsEnabledStatusEvent;
import org.wikipedia.events.ReadingListsMergeLocalDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;

public class SettingsFragment extends PreferenceLoaderFragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private SettingsPreferenceLoader preferenceLoader;
    private EventBusMethods busMethods;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        busMethods = new EventBusMethods();
        WikipediaApp.getInstance().getBus().register(busMethods);

        // TODO: Kick off a sync of reading lists, which will call back to us whether lists
        // are enabled or not. (Not sure if this is necessary yet.)
    }

    @Override public void onDestroy() {
        super.onDestroy();
        WikipediaApp.getInstance().getBus().unregister(busMethods);
        busMethods = null;
    }

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
        preferenceLoader.updateSyncReadingListsPrefSummary();
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

    private void setReadingListSyncPref(boolean checked) {
        if (preferenceLoader != null) {
            ((SwitchPreferenceCompat) preferenceLoader.findPreference(R.string.preference_key_sync_reading_lists))
                    .setChecked(checked);
        }
    }

    private class EventBusMethods {
        @Subscribe public void on(ReadingListsMergeLocalDialogEvent event) {
            setReadingListSyncPref(true);
        }

        @Subscribe public void on(ReadingListsEnabledStatusEvent event) {
            setReadingListSyncPref(true);
        }

        @Subscribe public void on(ReadingListsNoLongerSyncedEvent event) {
            setReadingListSyncPref(false);
        }
    }
}
