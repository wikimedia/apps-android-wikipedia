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

        // We use the ordinal() for passing the INVOKE_SOURCE into the intent because this intent
        // could be used as part of an App Shortcut, and unfortunately app shortcuts do not allow
        // Serializable objects in their intents.
        return new Intent(context, SearchActivity.class)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, source.ordinal())
                .putExtra(QUERY_EXTRA, query);
    }

    @Override
    public SearchFragment createFragment() {
        return SearchFragment.newInstance(InvokeSource.values()[getIntent().getIntExtra(INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.TOOLBAR.ordinal())],
                getIntent().getStringExtra(QUERY_EXTRA));
    }
}
