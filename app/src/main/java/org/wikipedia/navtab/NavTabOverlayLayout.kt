package org.wikipedia.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_nav_tab_overlay_fill.view.*
import org.wikipedia.R

class NavTabOverlayLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {
    init {
        View.inflate(context, R.layout.view_nav_tab_overlay, null)
    }

    fun pick(navTab: NavTab) {
        removeAllViews()
        for (i in 0 until NavTab.size()) {
            var childView = View(context)
            if (NavTab.of(i) == navTab) {
                childView = View.inflate(context, R.layout.view_nav_tab_overlay_fill, null)
                childView.pulsingBaseIcon.setImageResource(navTab.icon())
                childView.pulsingCircleOuter.startAnimation(AnimationUtils.loadAnimation(context, R.anim.pulsing_circle))
            }
            childView.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            addView(childView)
        }
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
