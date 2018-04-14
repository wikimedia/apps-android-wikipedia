package org.wikipedia.settings.languages;

import org.wikipedia.activity.SingleFragmentActivity;

public class WikipediaLanguagesActivity extends SingleFragmentActivity<WikipediaLanguagesFragment> {
    @Override
    protected WikipediaLanguagesFragment createFragment() {
        return WikipediaLanguagesFragment.newInstance();
    }
}
