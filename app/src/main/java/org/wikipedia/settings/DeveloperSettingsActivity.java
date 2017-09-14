package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;

import org.wikipedia.activity.SingleFragmentActivity;

public class DeveloperSettingsActivity extends SingleFragmentActivity<DeveloperSettingsFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }
}
