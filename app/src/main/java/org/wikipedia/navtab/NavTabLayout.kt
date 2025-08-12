package org.wikipedia.navtab

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateMarginsRelative
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class NavTabLayout(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        NavTab.entries.forEachIndexed { index, tab ->
            menu.add(Menu.NONE, tab.id, index, tab.text).setIcon(tab.icon)
        }
    }

    fun setOverlayDot(tab: NavTab, enabled: Boolean) {
        val itemView = findViewById<ViewGroup>(tab.id)
        val imageView = itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)
        val imageParent = imageView.parent as FrameLayout
        var overlayDotView: ImageView? = itemView.findViewById<ImageView?>(R.id.nav_tab_overlay_dot)
        if (overlayDotView == null) {
            overlayDotView = ImageView(context)
            overlayDotView.id = R.id.nav_tab_overlay_dot
            val dotSize = DimenUtil.roundedDpToPx(6f)
            val params = LayoutParams(dotSize, dotSize)
            params.gravity = Gravity.CENTER
            val margin = DimenUtil.roundedDpToPx(8f)
            params.updateMarginsRelative(start = margin, bottom = margin)
            overlayDotView.layoutParams = params
            overlayDotView.setBackgroundResource(R.drawable.shape_circle)
            overlayDotView.backgroundTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.destructive_color))
            imageParent.addView(overlayDotView)
        }
        overlayDotView.isVisible = enabled
    }
}
