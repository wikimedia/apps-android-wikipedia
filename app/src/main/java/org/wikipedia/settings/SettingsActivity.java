package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;

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
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED);
        }
    }
}
