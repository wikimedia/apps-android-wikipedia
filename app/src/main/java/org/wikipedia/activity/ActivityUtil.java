package org.wikipedia.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;

import org.wikipedia.util.ApiUtil;

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

    public static boolean defaultOnOptionsItemSelected(Activity activity, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.onBackPressed();
                return true;
            default:
                return false;
        }
    }

    public static Intent getLaunchIntent(Context context) {
        return getLaunchIntent(context, context.getPackageName());
    }

    @Nullable public static Intent getLaunchIntent(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }

    private ActivityUtil() { }
}