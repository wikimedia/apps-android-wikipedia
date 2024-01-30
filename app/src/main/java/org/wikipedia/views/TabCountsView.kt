package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewTabsCountBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class TabCountsView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewTabsCountBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(48.0f), ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackgroundBorderless))
        isFocusable = true
    }

    fun updateTabCount(animation: Boolean) {
        val count = WikipediaApp.instance.tabCount
        binding.tabsCountText.text = count.toString()
        val tabTextSize = if (count > 99) 5f else 10f
        binding.tabsCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, tabTextSize)
        if (animation) {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.tab_list_zoom_enter))
        }
    }
}
