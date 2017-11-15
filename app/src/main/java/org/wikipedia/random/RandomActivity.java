package org.wikipedia.random;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class RandomActivity extends SingleFragmentActivity<RandomFragment> {
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, RandomActivity.class);
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
