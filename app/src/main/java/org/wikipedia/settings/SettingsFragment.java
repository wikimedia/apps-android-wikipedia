package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent;
import org.wikipedia.events.ReadingListsEnabledStatusEvent;
import org.wikipedia.events.ReadingListsMergeLocalDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class SettingsFragment extends PreferenceLoaderFragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private SettingsPreferenceLoader preferenceLoader;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            requireActivity().finish();
            startActivity(requireActivity().getIntent());
        }

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        // TODO: Kick off a sync of reading lists, which will call back to us whether lists
        // are enabled or not. (Not sure if this is necessary yet.)
    }

    @Override public void onDestroy() {
        super.onDestroy();
        disposables.clear();
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
        requireActivity().getWindow().getDecorView().post(() -> {
            if (!isAdded()) {
                return;
            }
            preferenceLoader.updateSyncReadingListsPrefSummary();
            preferenceLoader.updateLanguagePrefSummary();
        });
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

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) throws Exception {
            if (event instanceof ReadingListsMergeLocalDialogEvent) {
                setReadingListSyncPref(true);
            } else if (event instanceof ReadingListsEnabledStatusEvent) {
                setReadingListSyncPref(true);
            } else if (event instanceof ReadingListsNoLongerSyncedEvent) {
                setReadingListSyncPref(false);
            } else if (event instanceof ReadingListsEnableSyncStatusEvent) {
                setReadingListSyncPref(Prefs.isReadingListSyncEnabled());
            }
        }
    }
}
