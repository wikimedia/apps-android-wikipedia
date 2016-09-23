package org.wikipedia.views;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

public class BottomAppBarLayoutBehavior extends CoordinatorLayout.Behavior<View> {
    public BottomAppBarLayoutBehavior() { }

    public BottomAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        super.layoutDependsOn(parent, child, dependency);
        return dependency instanceof AppBarLayout;
    }

    @Override public boolean onDependentViewChanged(CoordinatorLayout parent, View child,
                                                    View dependency) {
        super.onDependentViewChanged(parent, child, dependency);
        int translation = dependency.getHeight();
        float ratio = dependency.getY() / dependency.getHeight();
        child.setTranslationY(-translation * ratio);
        return true;
    }
}