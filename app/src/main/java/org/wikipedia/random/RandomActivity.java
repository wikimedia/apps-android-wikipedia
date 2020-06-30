package org.wikipedia.random;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.activity.SingleFragmentActivity;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class RandomActivity extends SingleFragmentActivity<RandomFragment> {

    public static Intent newIntent(@NonNull Context context, Constants.InvokeSource invokeSource) {
        return new Intent(context, RandomActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
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
