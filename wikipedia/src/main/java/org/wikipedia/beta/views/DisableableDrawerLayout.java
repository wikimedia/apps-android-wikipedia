package org.wikipedia.beta.views;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;

public class DisableableDrawerLayout extends DrawerLayout {

    public boolean getSlidingEnabled() {
        return getDrawerLockMode(Gravity.RIGHT) == DrawerLayout.LOCK_MODE_UNLOCKED;
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
