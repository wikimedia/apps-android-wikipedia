package org.wikipedia.feed.configure;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class ConfigureActivity extends SingleFragmentActivity<ConfigureFragment> {
    public static final int CONFIGURATION_CHANGED_RESULT = 1;

    public static Intent newIntent(@NonNull Context context, int invokeSource) {
        return new Intent(context, ConfigureActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    protected ConfigureFragment createFragment() {
        return ConfigureFragment.newInstance();
    }
}
