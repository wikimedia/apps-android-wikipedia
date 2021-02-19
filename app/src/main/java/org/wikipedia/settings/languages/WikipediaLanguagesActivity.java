package org.wikipedia.settings.languages;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.wikipedia.Constants;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;

public class WikipediaLanguagesActivity extends SingleFragmentActivity<WikipediaLanguagesFragment> {
    public static Intent newIntent(@NonNull Context context, @NonNull Constants.InvokeSource invokeSource) {
        return new Intent(context, WikipediaLanguagesActivity.class)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }
    @Override
    protected WikipediaLanguagesFragment createFragment() {
        return WikipediaLanguagesFragment.newInstance((Constants.InvokeSource) getIntent().getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE));
    }

    @Override protected void onDestroy() {
        // Regardless of why the activity is closing, let's explicitly refresh any
        // language-dependent widgets.
        WidgetProviderFeaturedPage.forceUpdateWidget(getApplicationContext());
        super.onDestroy();
    }
}
