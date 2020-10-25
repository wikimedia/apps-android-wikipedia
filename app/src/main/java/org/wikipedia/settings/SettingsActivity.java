package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_FEED_CONFIGURE;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;
    public static final int ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED = 2;

    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, SettingsActivity.class);
    }

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO not set result code when nothing is changed
        if (requestCode == ACTIVITY_REQUEST_FEED_CONFIGURE) {
            setResult(ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED);
        }
    }
}
