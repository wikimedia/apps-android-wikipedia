package org.wikipedia.feed.configure;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class ConfigureActivity extends SingleFragmentActivity<ConfigureFragment> {
    public static final int CONFIGURATION_CHANGED_RESULT = 1;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, ConfigureActivity.class);
    }

    @Override
    protected ConfigureFragment createFragment() {
        return ConfigureFragment.newInstance();
    }
}
