package org.wikipedia.search;

import android.content.Context;
import android.content.Intent;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.IntentFunnel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchActivity extends SingleFragmentActivity<SearchFragment> {
    static final String INVOKE_SOURCE_EXTRA = "invokeSource";
    static final String QUERY_EXTRA = "query";

    public static Intent newIntent(@NonNull Context context, int source, @Nullable String query) {

        if (source == SearchInvokeSource.WIDGET.code()) {
            new IntentFunnel(WikipediaApp.getInstance()).logSearchWidgetTap();
        }

        return new Intent(context, SearchActivity.class)
                .putExtra(INVOKE_SOURCE_EXTRA, source)
                .putExtra(QUERY_EXTRA, query);
    }

    @Override
    public SearchFragment createFragment() {
        return SearchFragment.newInstance(getIntent().getIntExtra(INVOKE_SOURCE_EXTRA, SearchInvokeSource.TOOLBAR.code()),
                getIntent().getStringExtra(QUERY_EXTRA));
    }
}
