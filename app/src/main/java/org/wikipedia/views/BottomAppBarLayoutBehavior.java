package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

public final class BottomAppBarLayoutBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private static final int SCROLL_DIRECTION_NONE = 0;
    private static final int SCROLL_DIRECTION_UP = 1;
    private static final int SCROLL_DIRECTION_DOWN = 2;
    private static final Interpolator INTERPOLATOR = new LinearOutSlowInInterpolator();

    private int totalDyUnconsumed = 0;
    private int totalDy = 0;
    private int scrollDirection = SCROLL_DIRECTION_NONE;
    private boolean hidden = false;
    private ViewPropertyAnimatorCompat offsetValueAnimator;

    public BottomAppBarLayoutBehavior() { }

    public BottomAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                               int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);
        if (dyUnconsumed > 0 && totalDyUnconsumed < 0) {
            totalDyUnconsumed = 0;
        } else if (dyUnconsumed < 0 && totalDyUnconsumed > 0) {
            totalDyUnconsumed = 0;
        }
        totalDyUnconsumed += dyUnconsumed;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx,
                                  int dy, int[] consumed) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        if (dy > 0 && totalDy < 0) {
            totalDy = 0;
            scrollDirection = SCROLL_DIRECTION_UP;
        } else if (dy < 0 && totalDy > 0) {
            totalDy = 0;
            scrollDirection = SCROLL_DIRECTION_DOWN;
        }
        totalDy += dy;
        animateOnScroll(child, scrollDirection);
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, V child, View target,
                                 float velocityX, float velocityY, boolean consumed) {
        super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
        scrollDirection = velocityY > 0 ? SCROLL_DIRECTION_UP : SCROLL_DIRECTION_DOWN;
        animateOnScroll(child, scrollDirection);
        return true;
    }

    public void show(@NonNull V child) {
        ensureOrCancelAnimator(child);
        offsetValueAnimator.translationY(0).start();
        hidden = false;
    }

    public void hide(@NonNull V child) {
        ensureOrCancelAnimator(child);
        offsetValueAnimator.translationY(child.getHeight()).start();
        hidden = true;
    }

    private void animateOnScroll(@NonNull V child, int scrollDirection) {
        if (scrollDirection == SCROLL_DIRECTION_DOWN && hidden) {
            show(child);
        } else if (scrollDirection == SCROLL_DIRECTION_UP && !hidden) {
            hide(child);
        }
    }

    private void ensureOrCancelAnimator(@NonNull V child) {
        if (offsetValueAnimator == null) {
            offsetValueAnimator = ViewCompat.animate(child);
            offsetValueAnimator.setDuration(child.getResources()
                    .getInteger(android.R.integer.config_shortAnimTime));
            offsetValueAnimator.setInterpolator(INTERPOLATOR);
        } else {
            offsetValueAnimator.cancel();
        }
    }
}
