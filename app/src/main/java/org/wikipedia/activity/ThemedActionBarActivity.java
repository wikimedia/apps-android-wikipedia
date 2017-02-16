package org.wikipedia.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.squareup.otto.Subscribe;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.events.WikipediaZeroEnterEvent;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.settings.Prefs;

import java.lang.reflect.Field;

public abstract class ThemedActionBarActivity extends BaseActivity {
    private boolean mDestroyed;
    private EventBusMethods busMethods;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        busMethods = new EventBusMethods();
        WikipediaApp.getInstance().getBus().register(busMethods);

        requestFullUserOrientation();

        setTheme();
        removeSplashBackground();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        forceOverflowMenuIcon(this);

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(WikipediaApp.getInstance()).run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccountUtil.logOutIfAccountRemoved();

        // The UI is likely shown, giving the user the opportunity to exit and making a crash loop
        // less probable.
        Prefs.crashedBeforeActivityCreated(false);
    }

    @Override
    public void onDestroy() {
        WikipediaApp.getInstance().getBus().unregister(busMethods);
        super.onDestroy();
        mDestroyed = true;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return super.isDestroyed();
        }
        return mDestroyed;
    }

    protected void setTheme() {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
    }

    protected void setActionBarTheme() {
        setTheme(WikipediaApp.getInstance().isCurrentThemeLight()
                ? R.style.Theme_Light_ActionBar
                : R.style.Theme_Dark_ActionBar);
    }

    /**
     * Helper function to force the Activity to show the three-dot overflow icon in its ActionBar.
     * @param activity Activity whose overflow icon will be forced.
     */
    private static void forceOverflowMenuIcon(Activity activity) {
        // API 19 is required for ReflectiveOperationException subclasses
        //noinspection TryWithIdenticalCatches
        try {
            ViewConfiguration config = ViewConfiguration.get(activity);
            // This field doesn't exist in 4.4, where the overflow icon is always shown:
            // https://android.googlesource.com/platform/frameworks/base.git/+/ea04f3cfc6e245fb415fd352ed0048cd940a46fe%5E!/
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (IllegalAccessException ignore) {
        } catch (NoSuchFieldException ignore) { }
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
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void removeSplashBackground() {
        getWindow().setBackgroundDrawable(null);
    }

    private class EventBusMethods {
        @Subscribe
        public void onZeroEnter(WikipediaZeroEnterEvent event) {
            if (Prefs.isZeroTutorialEnabled()) {
                Prefs.setZeroTutorialEnabled(false);
                WikipediaApp.getInstance().getWikipediaZeroHandler()
                        .showZeroTutorialDialog(ThemedActionBarActivity.this);
            }
        }
    }
}
