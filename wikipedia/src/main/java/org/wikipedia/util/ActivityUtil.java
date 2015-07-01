package org.wikipedia.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;

public final class ActivityUtil {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void requestFullUserOrientation(Activity activity) {
        if (ApiUtil.hasJellyBeanMr2()) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }

    public static View getRootView(Activity activity) {
        return activity.findViewById(android.R.id.content).getRootView();
    }

    public static View getMenuItemView(Activity activity, MenuItem item) {
        return activity.findViewById(item.getItemId());
    }

    private ActivityUtil() { }
}