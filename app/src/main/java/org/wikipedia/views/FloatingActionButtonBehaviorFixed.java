package org.wikipedia.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import org.wikipedia.util.log.L;

// TODO: Remove when this is fixed:
// https://code.google.com/p/android/issues/detail?id=222597
//
public class FloatingActionButtonBehaviorFixed extends FloatingActionButton.Behavior {

    public FloatingActionButtonBehaviorFixed() {
        super();
    }

    public FloatingActionButtonBehaviorFixed(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean getInsetDodgeRect(@NonNull CoordinatorLayout parent,
                                     @NonNull FloatingActionButton child, @NonNull Rect rect) {
        super.getInsetDodgeRect(parent, child, rect);
        if (!rect.intersect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom())) {
            L.logRemoteErrorIfProd(new IllegalArgumentException("Rect should intersect with child's bounds."));
        }
        return false;
    }
}