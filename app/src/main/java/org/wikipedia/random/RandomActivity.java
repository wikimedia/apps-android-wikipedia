package org.wikipedia.random;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class RandomActivity extends SingleFragmentActivity<RandomFragment> {
    public static final int INVOKE_SOURCE_FEED = 0;
    public static final int INVOKE_SOURCE_SHORTCUT = 1;
    static final String INVOKE_SOURCE_EXTRA = "invokeSource";

    public static Intent newIntent(@NonNull Context context, int invokeSource) {
        return new Intent(context, RandomActivity.class)
                .putExtra(INVOKE_SOURCE_EXTRA, invokeSource);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setElevation(0f);
    }

    @Override
    public RandomFragment createFragment() {
        return RandomFragment.newInstance();
    }
}
