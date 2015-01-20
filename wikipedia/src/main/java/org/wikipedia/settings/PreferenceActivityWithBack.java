package org.wikipedia.settings;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public abstract class PreferenceActivityWithBack extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(getPrefsTheme());
        super.onCreate(savedInstanceState);
    }

    public int getPrefsTheme() {
        final WikipediaApp app = WikipediaApp.getInstance();
        int currentTheme = app.getCurrentTheme();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                && currentTheme == WikipediaApp.THEME_DARK) {
            /*
                https://phabricator.wikimedia.org/T85809
                Checkboxes in settings aren't visible in dark mode on 4.x (prob. also on 3.x)
                so make background color a bit lighter. Not needed in GB and Lollipop.
            */
            currentTheme = R.style.Theme_WikiDark_Prefs;
        }
        return currentTheme;
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
