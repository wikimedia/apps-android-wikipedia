package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class SwipeRefreshLayoutWithScroll constructor(context: Context, attrs: AttributeSet?) : SwipeRefreshLayout(context, attrs) {

    var scrollableChild: View? = null

    override fun canChildScrollUp(): Boolean {
        return if (scrollableChild == null) {
            false
        } else scrollableChild!!.scrollY > 0
    }
}
