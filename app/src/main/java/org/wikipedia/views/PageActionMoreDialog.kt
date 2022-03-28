package org.wikipedia.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ItemMoreMenuBinding
import org.wikipedia.databinding.ViewPageActionMoreBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.customize.CustomizeToolbarActivity
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class PageActionMoreDialog(val callback: PageActionItem.Callback,
                           val currentTab: Tab,
                           val model: PageViewModel) : ExtendedBottomSheetDialogFragment() {

    private var _binding: ViewPageActionMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = ViewPageActionMoreBinding.inflate(inflater, container, false)

        binding.moreMenuForward.setOnClickListener {
            dismiss()
            callback.forwardClick()
        }

        binding.customizeToolbar.setOnClickListener {
            dismiss()
            startActivity(CustomizeToolbarActivity.newIntent(requireContext()))
        }

        Prefs.customizeToolbarMenuOrder.forEach {
            val view = ItemMoreMenuBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            view.id = item.hashCode()
            view.text = context?.getString(item.titleResId)
            view.setCompoundDrawablesWithIntrinsicBounds(item.iconResId, 0, 0, 0)
            view.setOnClickListener {
                dismiss()
                item.select(callback)
            }
            binding.moreMenuList.addView(view)
        }

        binding.moreMenuForward.visibility = if (currentTab.canGoForward()) VISIBLE else GONE

        for (i in 1 until binding.moreMenuList.childCount) {
            val view = binding.moreMenuList.getChildAt(i) as MaterialTextView
            val pageActionItem = PageActionItem.find(view.id)
            val enabled =
                model.page != null && (!model.shouldLoadAsMobileWeb || (model.shouldLoadAsMobileWeb && pageActionItem.isAvailableOnMobileWeb))
            when (pageActionItem) {
                PageActionItem.ADD_TO_WATCHLIST -> {
                    view.setText(if (model.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
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
        disableBackgroundDim()
        requireDialog().window?.let {
            DeviceUtil.setNavigationBarColor(it, ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight =
            (DimenUtil.displayHeightPx * 0.75).toInt()
    }

    companion object {
        @JvmStatic
        fun newInstance(callback: PageActionItem.Callback,
                        currentTab: Tab,
                        model: PageViewModel): PageActionMoreDialog {
            return PageActionMoreDialog(callback, currentTab, model)
        }
    }
}
