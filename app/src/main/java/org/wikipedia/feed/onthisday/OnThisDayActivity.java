package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.activity.SingleFragmentActivity;

public class OnThisDayActivity extends SingleFragmentActivity<OnThisDayFragment> {
    public static final String AGE = "age";
    public static final int INVOKE_SOURCE_CARD_BODY = 0;
    public static final int INVOKE_SOURCE_CARD_FOOTER = 1;
    static final String INVOKE_SOURCE_EXTRA = "invokeSource";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementTransitions();
    }
    public static Intent newIntent(@NonNull Context context, int age, int invokeSource) {
        return new Intent(context, OnThisDayActivity.class)
                .putExtra(AGE, age)
                .putExtra(INVOKE_SOURCE_EXTRA, invokeSource);
    }

    @Override
    protected OnThisDayFragment createFragment() {
        return OnThisDayFragment.newInstance(getIntent().getIntExtra(AGE, 0));
    }
}
