package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;

import org.wikipedia.activity.ThemedSingleFragmentActivity;

public class DeveloperSettingsActivity extends ThemedSingleFragmentActivity<DeveloperSettingsFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }
}