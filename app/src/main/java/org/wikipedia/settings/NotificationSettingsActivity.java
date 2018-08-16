package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class NotificationSettingsActivity extends SingleFragmentActivity<NotificationSettingsFragment> {
    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, NotificationSettingsActivity.class);
    }

    @Override
    public NotificationSettingsFragment createFragment() {
        return NotificationSettingsFragment.newInstance();
    }
}
