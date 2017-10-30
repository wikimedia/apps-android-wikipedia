package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class OnThisDayActivity extends SingleFragmentActivity<OnThisDayFragment> {

    public static final String AGE = "age";


    public static Intent newIntent(@NonNull Context context, int age) {
        return new Intent(context, OnThisDayActivity.class).putExtra(AGE, age);
    }

    @Override
    protected OnThisDayFragment createFragment() {
        return OnThisDayFragment.newInstance(getIntent().getIntExtra(AGE, 0));
    }
}
