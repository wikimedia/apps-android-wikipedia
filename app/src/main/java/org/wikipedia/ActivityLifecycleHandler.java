package org.wikipedia;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.wikipedia.analytics.eventplatform.EventPlatformClient;
import org.wikipedia.main.MainActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.Theme;

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private boolean haveMainActivity;
    private boolean anyActivityResumed;

    boolean haveMainActivity() {
        return haveMainActivity;
    }

    boolean isAnyActivityResumed() {
        return anyActivityResumed;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        WikipediaApp app = WikipediaApp.getInstance();
        if (activity instanceof MainActivity) {
            haveMainActivity = true;
        }
        if (Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Theme currentTheme = app.getCurrentTheme();
            switch (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if (!app.getCurrentTheme().isDark()) {
                        app.setCurrentTheme(!app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.BLACK : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if (app.getCurrentTheme().isDark()) {
                        app.setCurrentTheme(app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark() ? Theme.LIGHT : app.unmarshalTheme(Prefs.getPreviousThemeId()));
                        Prefs.setPreviousThemeId(currentTheme.getMarshallingId());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        anyActivityResumed = true;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        anyActivityResumed = false;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            haveMainActivity = false;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int i) {
        if (i == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            EventPlatformClient.flushCachedEvents();
        }
    }
}
