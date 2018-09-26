package org.wikipedia.activity;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;

public final class ActivityUtil {

    public static View getMenuItemView(Activity activity, MenuItem item) {
        return activity.findViewById(item.getItemId());
    }

    private ActivityUtil() { }
}
