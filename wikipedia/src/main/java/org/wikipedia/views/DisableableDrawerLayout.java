package org.wikipedia.views;

import android.content.*;
import android.support.v4.widget.*;
import android.util.*;

public class DisableableDrawerLayout extends DrawerLayout {

    public boolean getSlidingEnabled() {
        return getDrawerLockMode(this) == DrawerLayout.LOCK_MODE_UNLOCKED;
    }

    public void setSlidingEnabled(boolean enable) {
        if (enable) {
            this.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            this.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public DisableableDrawerLayout(Context context) {
        super(context);
    }

    public DisableableDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableableDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
