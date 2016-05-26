package org.wikipedia.feed;

import android.content.Context;
import android.content.Intent;

import org.wikipedia.activity.SingleFragmentActivity;

public class FeedActivity extends SingleFragmentActivity<FeedFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, FeedActivity.class);
    }

    @Override protected FeedFragment createFragment() {
        return FeedFragment.newInstance();
    }

    @Override protected void setTheme() { }
}