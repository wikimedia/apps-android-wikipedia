package org.wikipedia.activity;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import org.wikipedia.WikipediaApp;
import org.wikipedia.events.AppLangChangeEvent;
import org.wikipedia.events.WikipediaZeroEnterEvent;
import org.wikipedia.settings.Prefs;

import static org.wikipedia.util.ResourceUtil.setLocale;

public abstract class BaseActivity extends AppCompatActivity {
    private boolean destroyed;
    private EventBusMethods busMethods;

    @Override public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return super.isDestroyed();
        }
        return destroyed;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return false;
        }
    }

    protected void requestFullUserOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        busMethods = new EventBusMethods();
        WikipediaApp.getInstance().getBus().register(busMethods);

        // todo: largely eliminate concept of system language
        setLocale(this, WikipediaApp.getInstance().getAppOrSystemLanguageCode());
    }

    @Override protected void onDestroy() {
        WikipediaApp.getInstance().getBus().unregister(busMethods);
        busMethods = null;
        super.onDestroy();
        destroyed = true;
    }

    protected void setStatusBarColor(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, color));
        }
    }

    private class EventBusMethods {
        @Subscribe public void on(AppLangChangeEvent event) {
            recreate();
        }

        // todo: reevaluate lifecycle. the bus is active when this activity is paused and we show ui
        @Subscribe public void on(WikipediaZeroEnterEvent event) {
            if (Prefs.isZeroTutorialEnabled()) {
                Prefs.setZeroTutorialEnabled(false);
                WikipediaApp.getInstance().getWikipediaZeroHandler()
                        .showZeroTutorialDialog(BaseActivity.this);
            }
        }
    }
}
