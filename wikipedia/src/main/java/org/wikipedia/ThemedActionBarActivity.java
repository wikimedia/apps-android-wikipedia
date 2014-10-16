package org.wikipedia;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewConfiguration;
import java.lang.reflect.Field;

public abstract class ThemedActionBarActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState, false);
    }

    public void onCreate(Bundle savedInstanceState, boolean withActionBarOverlay) {
        super.onCreate(savedInstanceState);
        setTheme(WikipediaApp.getInstance().getCurrentTheme());

        if (withActionBarOverlay) {
            supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        forceOverflowMenuIcon(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            //for 2.3 it seems to be necessary to set this explicitly:
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(Utils.getThemedAttributeId(this, R.attr.actionbar_drawable)));
        }

        // Just setting the logo in the AndroidManifest.xml is not enough for all cases.
        // 1: It doesn't work for GB.
        // 2: (The transition between PageViewFragment and SearchArticlesFragment makes it temporarily reveal
        // the icon instead of the logo. So, setting the icon dynamically seems to be the best solution to
        // avoid those issues, while still having a different launcher icon.)
        getSupportActionBar().setIcon(R.drawable.search_w);
    }

    private static void forceOverflowMenuIcon(ActionBarActivity activity) {
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

}
