package org.wikipedia.beta.views;

import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class DisableableDrawerLayout extends DrawerLayout {

    public boolean getSlidingEnabled() {
        return getDrawerLockMode(getGravity()) == DrawerLayout.LOCK_MODE_UNLOCKED;
    }

    public void setSlidingEnabled(boolean enable) {
        if (enable) {
            this.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            this.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    private int gravity = -1;

    /**
     * Ascertain the gravity of the child drawer of this layout. When constructing the layout in XML,
     * the gravity is assigned as an attribute. However, in an RTL environment, the gravity may actually
     * be flipped. This will help us determine the actual gravity of the drawer at runtime.
     * @return Gravity of the drawer view that this layout contains.
     */
    private int getGravity() {
        if (gravity == -1) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                int childGravity = GravityCompat.getAbsoluteGravity(((LayoutParams) child.getLayoutParams()).gravity,
                                                                     ViewCompat.getLayoutDirection(child));
                if ((childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == (Gravity.LEFT & Gravity.HORIZONTAL_GRAVITY_MASK)) {
                    gravity = Gravity.LEFT;
                } else if ((childGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == (Gravity.RIGHT & Gravity.HORIZONTAL_GRAVITY_MASK)) {
                    gravity = Gravity.RIGHT;
                }
            }
        }
        return gravity;
    }

    public void openDrawer() {
        openDrawer(getGravity());
    }

    public void closeDrawer() {
        closeDrawer(getGravity());
    }

    public boolean isDrawerOpen() {
        return isDrawerOpen(getGravity());
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
