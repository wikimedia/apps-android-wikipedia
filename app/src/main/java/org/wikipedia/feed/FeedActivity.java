package org.wikipedia.feed;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.Toolbar;

import org.wikipedia.activity.SingleFragmentActivity;

public class FeedActivity extends SingleFragmentActivity<FeedFragment> implements FeedFragment.Callback {
    public static Intent newIntent(Context context) {
        return new Intent(context, FeedActivity.class);
    }

    @Override protected FeedFragment createFragment() {
        return FeedFragment.newInstance();
    }

    @Override protected void setTheme() { }

    @Override
    public void onAddToolbar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onRemoveToolbar(Toolbar toolbar) {
        setSupportActionBar(null);
    }
}