package org.wikipedia.watchlist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.item_watchlist_header.view.*
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

class WatchlistHeaderView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), View.OnClickListener {
    var callback: Callback? = null

    init {
        View.inflate(context, R.layout.item_watchlist_header, this)
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        filterButtonAll.setOnClickListener(this)
        filterButtonTalk.setOnClickListener(this)
        filterButtonPages.setOnClickListener(this)
        filterButtonOther.setOnClickListener(this)

        enableFilterButton(filterButtonAll)
    }

    fun enableByFilterMode(mode: Int) {
        enableFilterButton(when (mode) {
            WatchlistFragment.FILTER_MODE_PAGES -> {
                filterButtonPages
            }
            WatchlistFragment.FILTER_MODE_TALK -> {
                filterButtonTalk
            }
            WatchlistFragment.FILTER_MODE_OTHER -> {
                filterButtonOther
            }
            else -> {
                filterButtonAll
            }
        })
    }

    private fun enableFilterButton(button: TextView) {
        setFilterButtonDisabled(filterButtonAll)
        setFilterButtonDisabled(filterButtonTalk)
        setFilterButtonDisabled(filterButtonPages)
        setFilterButtonDisabled(filterButtonOther)
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
        enableFilterButton(v as TextView)
        when (v) {
            filterButtonAll -> {
                callback?.onSelectFilterAll()
            }
            filterButtonTalk -> {
                callback?.onSelectFilterTalk()
            }
            filterButtonPages -> {
                callback?.onSelectFilterPages()
            }
            filterButtonOther -> {
                callback?.onSelectFilterOther()
            }
        }
    }

    interface Callback {
        fun onSelectFilterAll()
        fun onSelectFilterTalk()
        fun onSelectFilterPages()
        fun onSelectFilterOther()
    }
}