package org.wikipedia.views

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import kotlinx.android.synthetic.main.view_tabs_count.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class TabCountsView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_tabs_count, this)
        layoutParams = ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(48.0f), ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackgroundBorderless))
        isFocusable = true
    }

    fun updateTabCount() {
        val count = WikipediaApp.getInstance().tabCount
        tabsCountText.text = count.toString()

        var tabTextSize = TAB_COUNT_TEXT_SIZE_MEDIUM

        if (count > TAB_COUNT_LARGE_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_SMALL
        } else if (count <= TAB_COUNT_SMALL_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_LARGE
        }

        tabsCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, tabTextSize)
    }

    fun setColor(@ColorInt color: Int) {
        tabsCountText.setTextColor(color)
        tabsCountText.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    companion object {
        private const val TAB_COUNT_LARGE_NUMBER = 99f
        private const val TAB_COUNT_SMALL_NUMBER = 9f
        private const val TAB_COUNT_TEXT_SIZE_LARGE = 12f
        private const val TAB_COUNT_TEXT_SIZE_MEDIUM = 10f
        private const val TAB_COUNT_TEXT_SIZE_SMALL = 8f
    }
}
