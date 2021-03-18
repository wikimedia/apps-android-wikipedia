package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ViewPageActionOverflowBinding
import org.wikipedia.page.tabs.Tab

class PageActionOverflowView(context: Context) : FrameLayout(context) {

    interface Callback {
        fun forwardClick()
        fun watchlistClick(isWatched: Boolean)
        fun talkClick()
        fun editHistoryClick()
        fun shareClick()
        fun newTabClick()
        fun feedClick()
    }

    private var binding = ViewPageActionOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null
    private var isWatched = false

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, callback: Callback?, currentTab: Tab, isMobileWeb: Boolean,
             isWatched: Boolean, hasWatchlistExpiry: Boolean) {
        this.callback = callback
        this.isWatched = isWatched
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            PopupWindowCompat.showAsDropDown(it, anchorView, 0, 0, Gravity.END)
        }
        binding.overflowForward.visibility = if (currentTab.canGoForward()) VISIBLE else GONE
        binding.overflowWatchlist.setText(if (isWatched) R.string.menu_page_remove_from_watchlist else R.string.menu_page_add_to_watchlist)
        binding.overflowWatchlist.setCompoundDrawablesWithIntrinsicBounds(getWatchlistIcon(isWatched, hasWatchlistExpiry), 0, 0, 0)
        binding.overflowWatchlist.visibility = if (!isMobileWeb && AccountUtil.isLoggedIn) VISIBLE else GONE
    }

    @DrawableRes
    private fun getWatchlistIcon(isWatched: Boolean, hasWatchlistExpiry: Boolean): Int {
        return if (isWatched && !hasWatchlistExpiry) {
            R.drawable.ic_star_24
        } else if (!isWatched) {
            R.drawable.ic_baseline_star_outline_24
        } else {
            R.drawable.ic_baseline_star_half_24
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }

    private fun setButtonsListener() {
        binding.overflowForward.setOnClickListener {
            dismissPopupWindowHost()
            callback?.forwardClick()
        }
        binding.overflowShare.setOnClickListener {
            dismissPopupWindowHost()
            callback?.shareClick()
        }
        binding.overflowWatchlist.setOnClickListener {
            dismissPopupWindowHost()
            callback?.watchlistClick(isWatched)
        }
        binding.overflowTalk.setOnClickListener {
            dismissPopupWindowHost()
            callback?.talkClick()
        }
        binding.overflowEditHistory.setOnClickListener {
            dismissPopupWindowHost()
            callback?.editHistoryClick()
        }
        binding.overflowFeed.setOnClickListener {
            dismissPopupWindowHost()
            callback?.feedClick()
        }
        binding.overflowNewTab.setOnClickListener {
            dismissPopupWindowHost()
            callback?.newTabClick()
        }
    }
}
