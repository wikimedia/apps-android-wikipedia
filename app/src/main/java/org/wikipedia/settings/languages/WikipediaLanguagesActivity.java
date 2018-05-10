package org.wikipedia.settings.languages;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;

public class WikipediaLanguagesActivity extends SingleFragmentActivity<WikipediaLanguagesFragment> {
    @Override
    protected WikipediaLanguagesFragment createFragment() {
        return WikipediaLanguagesFragment.newInstance();
    }

    @Override protected void onDestroy() {
        // Regardless of why the activity is closing, let's explicitly refresh any
        // language-dependent widgets.
        WidgetProviderFeaturedPage.forceUpdateWidget(getApplicationContext());
        super.onDestroy();
    }
}
