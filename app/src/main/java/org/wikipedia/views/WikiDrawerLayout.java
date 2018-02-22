package org.wikipedia.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.FixedDrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import org.wikipedia.util.log.L;

import java.lang.reflect.Field;

/**
 * A thin wrapper around {@link FixedDrawerLayout} with additional functionality:
 * <ul>
 *   <li>Expose enable state.</li>
 *   <li>Expose drag margin width state.</li>
 * </ul>
 */
public class WikiDrawerLayout extends FixedDrawerLayout {
    public WikiDrawerLayout(Context context) {
        this(context, null);
    }

    public WikiDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

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

    /**
     * Set the drag margin width.
     * @param width Width in pixels.
     */
    public void setDragEdgeWidth(final int width) {
        this.post(() -> {
            try {
                // Use a little bit of reflection to set a private member in DrawerLayout that extends the
                // "drag edge" from which the drawer can be pulled by the user.
                // A bit hacky, but what are you gonna do...
                View pullOutView = getChildAt(1);
                int absGravity = GravityCompat.getAbsoluteGravity(((LayoutParams)pullOutView.getLayoutParams()).gravity,
                                                                  ViewCompat.getLayoutDirection(pullOutView));
                // Determine whether to modify the left or right dragger, based on RTL/LTR orientation
                @SuppressLint("RtlHardcoded")
                Field mDragger = (absGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT
                        ? WikiDrawerLayout.this.getClass().getSuperclass().getSuperclass().getDeclaredField("mLeftDragger")
                        : WikiDrawerLayout.this.getClass().getSuperclass().getSuperclass().getDeclaredField("mRightDragger");
                mDragger.setAccessible(true);
                ViewDragHelper dragHelper = (ViewDragHelper) mDragger.get(WikiDrawerLayout.this);
                Field edgeWidth = dragHelper.getClass().getDeclaredField("mEdgeSize");
                edgeWidth.setAccessible(true);
                edgeWidth.setInt(dragHelper, width);
            } catch (Exception e) {
                L.e("Setting the draggable zone for the drawer failed!", e);
            }
        });
    }
}
