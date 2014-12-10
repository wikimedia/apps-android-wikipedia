package org.wikipedia;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import java.lang.reflect.Field;

public abstract class ThemedActionBarActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(WikipediaApp.getInstance().getCurrentTheme());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        forceOverflowMenuIcon(this);
    }

    /**
     * Helper function to force the Activity to show the three-dot overflow icon in its ActionBar.
     * @param activity Activity whose overflow icon will be forced.
     */
    private static void forceOverflowMenuIcon(Activity activity) {
        try {
            ViewConfiguration config = ViewConfiguration.get(activity);
            // Note: this field doesn't exist in 2.3, so those users will need to tap the physical menu button,
            // unless we figure out another solution.
            // This field also doesn't exist in 4.4, where the overflow icon is always shown:
            // https://android.googlesource.com/platform/frameworks/base.git/+/ea04f3cfc6e245fb415fd352ed0048cd940a46fe%5E!/
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // multiple exceptions may be thrown above, but it's not super critical if it fails.
        }
    }

    // Hack for https://phabricator.wikimedia.org/T78117: onKeyDown + onKeyUp
    // Consider removing once updating appcompat-v7.

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
