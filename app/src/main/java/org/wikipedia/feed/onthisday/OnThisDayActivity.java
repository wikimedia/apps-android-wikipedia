package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.dataclient.WikiSite;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class OnThisDayActivity extends SingleFragmentActivity<OnThisDayFragment> {
    public static final String AGE = "age";
    public static final String WIKISITE = "wikisite";

    public static Intent newIntent(@NonNull Context context, int age, WikiSite wikiSite, InvokeSource invokeSource) {
        return new Intent(context, OnThisDayActivity.class)
                .putExtra(AGE, age)
                .putExtra(WIKISITE, wikiSite)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getFragment().onBackPressed();
    }

    @Override
    protected OnThisDayFragment createFragment() {
        return OnThisDayFragment.newInstance(getIntent().getIntExtra(AGE, 0), getIntent().getParcelableExtra(WIKISITE));
    }
}
