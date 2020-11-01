package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.util.StringUtil;

import java.util.List;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_FEED_CONFIGURE;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;
    public static final int ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED = 2;

    private WikipediaApp app = WikipediaApp.getInstance();

    private String initialLanguageList;
    private List<Boolean> initialFeedCardsEnabled;
    private List<Integer> initialFeedCardsOrder;

    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, SettingsActivity.class);
    }

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialLanguageList = StringUtil.listToJsonArrayString(app.language().getAppLanguageCodes());
        initialFeedCardsEnabled = Prefs.getFeedCardsEnabled();
        initialFeedCardsOrder = Prefs.getFeedCardsOrder();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String finalLanguageList = StringUtil.listToJsonArrayString(app.language().getAppLanguageCodes());
        List<Boolean> finalFeedCardsEnabled = Prefs.getFeedCardsEnabled();
        List<Integer> finalFeedCardsOrder = Prefs.getFeedCardsOrder();

        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE
                && (!finalLanguageList.equals(initialLanguageList))) {
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED);
        } else if (requestCode == ACTIVITY_REQUEST_FEED_CONFIGURE
                && (!initialFeedCardsEnabled.equals(finalFeedCardsEnabled)
                || !initialFeedCardsOrder.equals(finalFeedCardsOrder))) {
            setResult(ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED);
        }
    }
}
