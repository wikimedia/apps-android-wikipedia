package org.wikipedia.watchlist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemWatchlistHeaderBinding
import org.wikipedia.util.ResourceUtil

class WatchlistHeaderView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), View.OnClickListener {
    val binding = ItemWatchlistHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: Callback? = null

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.filterButtonAllContainer.setOnClickListener(this)
        binding.filterButtonTalkContainer.setOnClickListener(this)
        binding.filterButtonPagesContainer.setOnClickListener(this)
        binding.filterButtonOtherContainer.setOnClickListener(this)

        enableFilterButton(binding.filterButtonAll)
    }

    fun enableByFilterMode(mode: Int) {
        enableFilterButton(when (mode) {
            WatchlistFragment.FILTER_MODE_PAGES -> binding.filterButtonPages
            WatchlistFragment.FILTER_MODE_TALK -> binding.filterButtonTalk
            WatchlistFragment.FILTER_MODE_OTHER -> binding.filterButtonOther
            else -> binding.filterButtonAll
        })
    }

    private fun enableFilterButton(button: TextView) {
        setFilterButtonDisabled(binding.filterButtonAll)
        setFilterButtonDisabled(binding.filterButtonTalk)
        setFilterButtonDisabled(binding.filterButtonPages)
        setFilterButtonDisabled(binding.filterButtonOther)
        setFilterButtonEnabled(button)
    }

    private fun setFilterButtonEnabled(button: TextView) {
        button.setTextColor(ResourceUtil.getThemedColor(context, R.attr.themed_icon_color))
        button.background = ContextCompat.getDrawable(context, R.drawable.rounded_20dp_accent90_fill)
    }

    private fun setFilterButtonDisabled(button: TextView) {
        button.setTextColor(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color))
        button.background = ContextCompat.getDrawable(context, R.drawable.rounded_20dp_base90_fill)
    }

    override fun onClick(v: View?) {
        enableFilterButton((v as LinearLayout).getChildAt(0) as TextView)
        when (v) {
            binding.filterButtonAllContainer -> callback?.onSelectFilterAll()
            binding.filterButtonTalkContainer -> callback?.onSelectFilterTalk()
            binding.filterButtonPagesContainer -> callback?.onSelectFilterPages()
            binding.filterButtonOtherContainer -> callback?.onSelectFilterOther()
        }
    }

    interface Callback {
        fun onSelectFilterAll()
        fun onSelectFilterTalk()
        fun onSelectFilterPages()
        fun onSelectFilterOther()
    }
}
