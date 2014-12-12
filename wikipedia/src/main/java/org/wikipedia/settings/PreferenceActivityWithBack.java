package org.wikipedia.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import org.wikipedia.WikipediaApp;

public abstract class PreferenceActivityWithBack extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme());
        super.onCreate(savedInstanceState);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
        }
    }

}
