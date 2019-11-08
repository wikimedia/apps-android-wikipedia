package org.wikipedia;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import org.jetbrains.annotations.NotNull;
import org.wikipedia.main.MainActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.Theme;

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private boolean haveMainActivity;
    private boolean anyActivityResumed;

    public boolean haveMainActivity() {
        return haveMainActivity;
    }

    public boolean isAnyActivityResumed() {
        return anyActivityResumed;
    }

    @Override
    public void onActivityCreated(@NotNull Activity activity, Bundle savedInstanceState) {
        if (activity instanceof MainActivity) {
            haveMainActivity = true;
        }
        if (Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            switch (WikipediaApp.getInstance().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if (!WikipediaApp.getInstance().getCurrentTheme().isDark()) {
                        WikipediaApp.getInstance().setCurrentTheme(Theme.BLACK);
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if (WikipediaApp.getInstance().getCurrentTheme().isDark()) {
                        WikipediaApp.getInstance().setCurrentTheme(Theme.LIGHT);
                    }
                    break;
                default:
                    WikipediaApp.getInstance().setCurrentTheme(Theme.LIGHT);
            }
        }
    }

    @Override
    public void onActivityStarted(@NotNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NotNull Activity activity) {
        anyActivityResumed = true;
    }

    @Override
    public void onActivityPaused(@NotNull Activity activity) {
        anyActivityResumed = false;
    }

    @Override
    public void onActivityStopped(@NotNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NotNull Activity activity) {
        if (activity instanceof MainActivity) {
            haveMainActivity = false;
        }
    }
}
