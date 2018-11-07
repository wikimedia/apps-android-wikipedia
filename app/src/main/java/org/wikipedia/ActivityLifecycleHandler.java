package org.wikipedia;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.wikipedia.main.MainActivity;

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private boolean haveMainActivity;

    public boolean haveMainActivity() {
        return haveMainActivity;
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
    }

    @Override
    public void onActivityPaused(Activity activity) {
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
