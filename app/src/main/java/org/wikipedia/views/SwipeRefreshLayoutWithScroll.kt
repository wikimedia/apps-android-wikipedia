package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class SwipeRefreshLayoutWithScroll @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null)
    : SwipeRefreshLayout(context, attrs) {

    private var scrollableView: View? = null

    fun setScrollableChild(scrollableView: View?) {
        this.scrollableView = scrollableView
    }

    override fun canChildScrollUp(): Boolean = scrollableView?.let { it.scrollY > 0 } ?: false

    /**
     * TODO: Remove this override when it's fixed in support-v4.
     * https://phabricator.wikimedia.org/T88904
     *
     * This seems to have been fixed in the Support library, but not released yet:
     * https://github.com/android/platform_frameworks_support/commit/07a4db40e79aae23694b205f99b013ee2e4f2307
     */
    override fun onTouchEvent(event: MotionEvent): Boolean =
            try {
                super.onTouchEvent(event)
            } catch (e: RuntimeException) {
                false
            }
}
