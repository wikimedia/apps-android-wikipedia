package org.wikipedia.random;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

public class BottomViewBehavior extends CoordinatorLayout.Behavior<ViewGroup> {

    private static final long ANIMATION_DURATION_MILLISECONDS = 100;

    public BottomViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull ViewGroup child, @NonNull View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull ViewGroup child, @NonNull View dependency) {
        child.animate().setDuration(ANIMATION_DURATION_MILLISECONDS).translationY(-dependency.getHeight());
        return true;
    }

    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull ViewGroup child,
                                       @NonNull View dependency) {
        child.animate().setDuration(ANIMATION_DURATION_MILLISECONDS).translationY(0);
    }
}
