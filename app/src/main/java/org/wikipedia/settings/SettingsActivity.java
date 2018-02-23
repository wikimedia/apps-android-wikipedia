package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;

    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, SettingsActivity.class);
    }

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }


    @Override
    protected void onOfflineCompilationsFound() {
        getFragment().updateOfflineLibraryPref(true);
    }

    @Override
    protected void onOfflineCompilationsError(Throwable t) {
        getFragment().updateOfflineLibraryPref(false);
    }
}
