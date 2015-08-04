package org.wikipedia.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.ActivityUtil;

/*package*/ abstract class LegacyPreferenceActivity extends PreferenceActivity
        implements PreferenceLoader {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
        super.onCreate(savedInstanceState);
        loadPreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }
}