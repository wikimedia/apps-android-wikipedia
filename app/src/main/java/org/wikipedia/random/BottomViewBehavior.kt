package org.wikipedia.random

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar.SnackbarLayout

class BottomViewBehavior(
        context: Context?, attrs: AttributeSet?
) : CoordinatorLayout.Behavior<ViewGroup>(context, attrs) {

    companion object {
        private const val ANIMATION_DURATION_MILLISECONDS = 100L
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: ViewGroup, dependency: View): Boolean {
        return dependency is SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: ViewGroup, dependency: View): Boolean {
        child.animate().setDuration(ANIMATION_DURATION_MILLISECONDS)
                .translationY(-dependency.height.toFloat())
        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: ViewGroup, dependency: View) {
        child.animate().setDuration(ANIMATION_DURATION_MILLISECONDS)
                .translationY(0f)
    }
}
