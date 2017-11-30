package org.wikipedia.feed.configure;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class ConfigureActivity extends SingleFragmentActivity<ConfigureFragment> {
    public static final int CONFIGURATION_CHANGED_RESULT = 1;
    static final String INVOKE_SOURCE_EXTRA = "invokeSource";

    public static Intent newIntent(@NonNull Context context, int invokeSource) {
        return new Intent(context, ConfigureActivity.class)
                .putExtra(INVOKE_SOURCE_EXTRA, invokeSource);
    }

    @Override
    protected ConfigureFragment createFragment() {
        return ConfigureFragment.newInstance();
    }
}
