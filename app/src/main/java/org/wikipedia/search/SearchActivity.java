package org.wikipedia.search;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.IntentFunnel;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource.WIDGET;

public class SearchActivity extends SingleFragmentActivity<SearchFragment> {
    static final String QUERY_EXTRA = "query";

    public static Intent newIntent(@NonNull Context context, InvokeSource source, @Nullable String query) {

        if (source == WIDGET) {
            new IntentFunnel(WikipediaApp.getInstance()).logSearchWidgetTap();
        }

        return new Intent(context, SearchActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, source)
                .putExtra(QUERY_EXTRA, query);
    }

    @Override
    public SearchFragment createFragment() {
        return SearchFragment.newInstance((InvokeSource) getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE),
                getIntent().getStringExtra(QUERY_EXTRA));
    }
}
