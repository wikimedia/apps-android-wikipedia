package org.wikipedia;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.wikipedia.main.MainActivity;

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
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof MainActivity) {
            haveMainActivity = true;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        anyActivityResumed = true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        anyActivityResumed = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof MainActivity) {
            haveMainActivity = false;
        }
    }
}
