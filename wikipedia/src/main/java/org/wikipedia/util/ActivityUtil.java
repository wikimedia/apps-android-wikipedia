package org.wikipedia.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public final class ActivityUtil {
    public static void requestFullUserOrientation(Activity activity) {
        if (ApiUtil.hasJellyBeanMr2()) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }

    private ActivityUtil() {
    }
}