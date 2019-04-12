package org.wikipedia.main.floatingqueue;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.random.BottomViewBehavior;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class FloatingQueueBottomViewBehavior extends BottomViewBehavior {
    public FloatingQueueBottomViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, ViewGroup child, View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight() - dependency.getResources()
                .getDimension(R.dimen.floating_queue_container_margin_top_bottom));
        child.setTranslationY(translationY);
        return true;
    }
}
