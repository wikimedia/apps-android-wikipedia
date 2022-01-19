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
import androidx.core.widget.PopupWindowCompat
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ItemCustomizeToolbarMenuBinding
import org.wikipedia.databinding.ViewPageActionOverflowBinding
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs

class PageActionOverflowView(context: Context) : FrameLayout(context) {

    private var binding = ViewPageActionOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var popupWindowHost: PopupWindow? = null
    lateinit var callback: PageActionItem.Callback

    init {
        binding.overflowForward.setOnClickListener {
            dismissPopupWindowHost()
            callback.forwardClick()
        }
        Prefs.customizeToolbarMenuOrder.forEach {
            val view = ItemCustomizeToolbarMenuBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            view.id = item.hashCode()
            view.text = context.getString(item.titleResId)
            view.setCompoundDrawablesWithIntrinsicBounds(item.iconResId, 0, 0, 0)
            view.setOnClickListener {
                dismissPopupWindowHost()
                item.select(callback)
            }
            binding.overflowList.addView(view)
        }
    }

    fun show(anchorView: View, callback: PageActionItem.Callback, currentTab: Tab, model: PageViewModel) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }
        binding.overflowForward.visibility = if (currentTab.canGoForward()) VISIBLE else GONE

        for (i in 1 until binding.overflowList.childCount) {
            val view = binding.overflowList.getChildAt(i) as MaterialTextView
            val pageActionItem = PageActionItem.find(view.id)
            val enabled = model.page != null && (!model.shouldLoadAsMobileWeb || (model.shouldLoadAsMobileWeb && pageActionItem.isAvailableOnMobileWeb))
            when (pageActionItem) {
                PageActionItem.ADD_TO_WATCHLIST -> {
                    view.setText(if (model.isWatched) R.string.menu_page_remove_from_watchlist else R.string.menu_page_add_to_watchlist)
                    view.setCompoundDrawablesWithIntrinsicBounds(PageActionItem.watchlistIcon(model.isWatched, model.hasWatchlistExpiry), 0, 0, 0)
                    view.visibility = if (enabled && AccountUtil.isLoggedIn) VISIBLE else GONE
                }
                PageActionItem.SAVE -> {
                    view.setCompoundDrawablesWithIntrinsicBounds(PageActionItem.readingListIcon(model.isInReadingList), 0, 0, 0)
                    view.visibility = if (enabled) VISIBLE else GONE
                }
                else -> {
                    view.visibility = if (enabled) VISIBLE else GONE
                }
            }
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }
}
