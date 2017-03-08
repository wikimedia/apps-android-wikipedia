package org.wikipedia.views;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public final class BottomAppBarLayoutBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private boolean snackBarShowing = false;

    public BottomAppBarLayoutBehavior() { }

    public BottomAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) dependency.getLayoutParams();
            layoutParams.bottomMargin = child.getMeasuredHeight();
            child.bringToFront();
            return true;
        }
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, V child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            snackBarShowing = false;
        }
        super.onDependentViewRemoved(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) {
        if ((dependency instanceof Snackbar.SnackbarLayout) && !snackBarShowing) {
            snackBarShowing = true;
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }
}
