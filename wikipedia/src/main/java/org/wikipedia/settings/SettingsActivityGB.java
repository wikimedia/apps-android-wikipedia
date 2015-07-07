package org.wikipedia.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.wikipedia.WikipediaApp;

/**
 * Settings activity that is specifically intended for API 10.
 * It's functionally identical to the real SettingsActivity, except that this one inherits from
 * PreferenceActivity, which was deprecated after API 10. The new SettingsActivity inherits from
 * ActionBarActivity, and uses a PreferenceFragment, all of which are necessary for all the
 * components to render properly (specifically checkboxes).
 */
public class SettingsActivityGB extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
        super.onCreate(savedInstanceState);

        PreferenceHostCompat prefHost = new PreferenceHostCompat(this);
        SettingsUI ui = new SettingsUI(this, prefHost);
        ui.loadPreferences();
    }

    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
        }
    }
}
