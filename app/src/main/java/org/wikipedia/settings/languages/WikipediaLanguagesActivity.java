package org.wikipedia.settings.languages;

import android.content.Intent;
import android.os.Bundle;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;

import static org.wikipedia.settings.languages.WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA;
import static org.wikipedia.views.LanguageScrollView.SELECTED_TAB_LANGUAGE_CODE;

public class WikipediaLanguagesActivity extends SingleFragmentActivity<WikipediaLanguagesFragment> {

    String searchTabLanguageCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                searchTabLanguageCode = extras.getString(SELECTED_TAB_LANGUAGE_CODE);
            }
        } else {
            searchTabLanguageCode = savedInstanceState.getString(SELECTED_TAB_LANGUAGE_CODE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_TAB_LANGUAGE_CODE, searchTabLanguageCode);
    }

    @Override
    protected WikipediaLanguagesFragment createFragment() {
        return WikipediaLanguagesFragment.newInstance();
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        int position = (WikipediaApp.getInstance().language().getAppLanguageCodes().contains(searchTabLanguageCode) ? WikipediaApp.getInstance().language().getAppLanguageCodes().indexOf(searchTabLanguageCode) : 0);
        resultIntent.putExtra(ACTIVITY_RESULT_LANG_POSITION_DATA, position);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
