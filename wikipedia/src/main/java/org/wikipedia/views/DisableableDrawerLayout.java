package org.wikipedia.views;

import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.FixedDrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import java.lang.reflect.Field;

public class DisableableDrawerLayout extends FixedDrawerLayout {

    public boolean getSlidingEnabled(int gravity) {
        return getDrawerLockMode(gravity) == DrawerLayout.LOCK_MODE_UNLOCKED;
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
        setDragEdgeWidth();
    }

    public DisableableDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDragEdgeWidth();
    }

    public DisableableDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDragEdgeWidth();
    }

    /**
     * The logical width (in dp) of the drag edge from which the user can drag out the drawer.
     */
    private static final int DRAG_EDGE_WIDTH = 48;

    private void setDragEdgeWidth() {
        this.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Use a little bit of reflection to set a private member in DrawerLayout that extends the
                    // "drag edge" from which the drawer can be pulled by the user.
                    // A bit hacky, but what are you gonna do...
                    View pullOutView = getChildAt(1);
                    int absGravity = GravityCompat.getAbsoluteGravity(((LayoutParams)pullOutView.getLayoutParams()).gravity,
                                                                      ViewCompat.getLayoutDirection(pullOutView));
                    // Determine whether to modify the left or right dragger, based on RTL/LTR orientation
                    Field mDragger = (absGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT
                            ? DisableableDrawerLayout.this.getClass().getSuperclass().getSuperclass().getDeclaredField("mLeftDragger")
                            : DisableableDrawerLayout.this.getClass().getSuperclass().getSuperclass().getDeclaredField("mRightDragger");
                    mDragger.setAccessible(true);
                    ViewDragHelper dragHelper = (ViewDragHelper) mDragger.get(DisableableDrawerLayout.this);
                    Field edgeWidth = dragHelper.getClass().getDeclaredField("mEdgeSize");
                    edgeWidth.setAccessible(true);
                    edgeWidth.setInt(dragHelper, (int)(DRAG_EDGE_WIDTH * getResources().getDisplayMetrics().density));
                } catch (Exception e) {
                    Log.e("DisableableDrawerLayout", "Setting the draggable zone for the drawer failed!", e);
                }
            }
        });
    }

}
