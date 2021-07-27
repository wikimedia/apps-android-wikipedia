package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewTabsCountBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class TabCountsView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewTabsCountBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(DimenUtil.roundedDpToPx(48.0f), ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackgroundBorderless))
        isFocusable = true
    }

    fun updateTabCount(animation: Boolean) {
        val count = WikipediaApp.instance.tabCount
        binding.tabsCountText.text = count.toString()

        var tabTextSize = TAB_COUNT_TEXT_SIZE_MEDIUM

        if (count > TAB_COUNT_LARGE_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_SMALL
        } else if (count <= TAB_COUNT_SMALL_NUMBER) {
            tabTextSize = TAB_COUNT_TEXT_SIZE_LARGE
        }

        binding.tabsCountText.setTextSize(TypedValue.COMPLEX_UNIT_PX, DimenUtil.dpToPx(tabTextSize))

        if (animation) {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.tab_list_zoom_enter))
        }
    }

    fun setColor(@ColorInt color: Int) {
        binding.tabsCountText.setTextColor(color)
        ViewCompat.setBackgroundTintList(binding.tabsCountText, ColorStateList.valueOf(color))
    }

    companion object {
        private const val TAB_COUNT_LARGE_NUMBER = 99f
        private const val TAB_COUNT_SMALL_NUMBER = 9f
        private const val TAB_COUNT_TEXT_SIZE_LARGE = 12f
        private const val TAB_COUNT_TEXT_SIZE_MEDIUM = 10f
        private const val TAB_COUNT_TEXT_SIZE_SMALL = 8f
    }
}
