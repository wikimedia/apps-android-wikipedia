package org.wikipedia.activity;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public final class ActivityUtil {
    public static View getRootView(Activity activity) {
        return activity.findViewById(android.R.id.content).getRootView();
    }

    public static View getMenuItemView(Activity activity, MenuItem item) {
        return activity.findViewById(item.getItemId());
    }

    /**
     * Helper function to force the Activity to show the three-dot overflow icon in its ActionBar.
     * @param activity Activity whose overflow icon will be forced.
     */
    public static void forceOverflowMenuIcon(Activity activity) {
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

    private ActivityUtil() { }
}
