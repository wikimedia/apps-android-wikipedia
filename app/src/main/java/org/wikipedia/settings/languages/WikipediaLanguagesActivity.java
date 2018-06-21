package org.wikipedia.settings.languages;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;

public class WikipediaLanguagesActivity extends SingleFragmentActivity<WikipediaLanguagesFragment> {
    static final String INVOKE_SOURCE_EXTRA = "invokeSource";

    public static Intent newIntent(@NonNull Context context, @NonNull String invokeSource) {
        return new Intent(context, WikipediaLanguagesActivity.class)
                .putExtra(INVOKE_SOURCE_EXTRA, invokeSource);
    }
    @Override
    protected WikipediaLanguagesFragment createFragment() {
        return WikipediaLanguagesFragment.newInstance(getIntent().getStringExtra(INVOKE_SOURCE_EXTRA));
    }

    @Override protected void onDestroy() {
        // Regardless of why the activity is closing, let's explicitly refresh any
        // language-dependent widgets.
        WidgetProviderFeaturedPage.forceUpdateWidget(getApplicationContext());
        super.onDestroy();
    }
}
