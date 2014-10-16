package org.wikipedia;

import com.nineoldandroids.view.ViewHelper;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import java.lang.reflect.Field;

public abstract class ThemedActionBarActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState, false, false);
    }

    public void onCreate(Bundle savedInstanceState, boolean withActionBarOverlay, boolean withProgressBar) {
        super.onCreate(savedInstanceState);
        setTheme(WikipediaApp.getInstance().getCurrentTheme());

        if (withProgressBar) {
            supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        }
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

    /**
     * Helper function to force the Activity to show the three-dot overflow icon in its ActionBar.
     * @param activity Activity whose overflow icon will be forced.
     */
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

    /**
     * Helper function to move the built-in ProgressBar that is part of the ActionBarActivity
     * from the very top of the activity to the bottom of the ActionBar, where it looks better.
     * Note: this only applies to API >10, since in API 10 the ProgressBar seems to be really
     * wide, and actually looks better at the top of the activity.
     * @param activity Activity whose ProgressBar to move.
     */
    public static void alignActivityProgressBar(ActionBarActivity activity) {
        ViewGroup actionBar;
        View progressBar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            actionBar = (ViewGroup)activity.getWindow()
                                           .getDecorView()
                                           .findViewById(Resources.getSystem().getIdentifier("action_bar_container", "id", "android"));
            progressBar = activity.getWindow()
                                  .getDecorView()
                                  .findViewById(Resources.getSystem().getIdentifier("progress_horizontal", "id", "android"));
            TypedValue tv = new TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            {
                // get the default height of the ActionBar
                int offsetHeight = TypedValue.complexToDimensionPixelSize(tv.data, activity.getResources().getDisplayMetrics());
                // subtract just a little bit, so that the whole ProgressBar fits at the bottom of the ActionBar
                offsetHeight -= (int)(2 * (activity.getResources().getDisplayMetrics().density));
                // and modify the offset of the ProgressBar!
                if (actionBar != null && progressBar != null && offsetHeight > 0) {
                    ViewHelper.setTranslationY(progressBar, offsetHeight);
                }
            }
        } else {
            // but just in case we'll want to do this in 2.3, this is how to get the View ids:
            /*
            actionBar = (ViewGroup)activity.getWindow()
                                           .getDecorView()
                                           .findViewById(activity.getResources().getIdentifier("action_bar_container", "id", activity.getPackageName()));
            progressBar = activity.getWindow()
                                  .getDecorView()
                                  .findViewById(activity.getResources().getIdentifier("progress_horizontal", "id", activity.getPackageName()));
            */
        }

    }
}
