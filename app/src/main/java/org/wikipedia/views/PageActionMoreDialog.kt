package org.wikipedia.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ItemCustomizeToolbarMenuBinding
import org.wikipedia.databinding.ViewPageActionOverflowBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.customize.CustomizeToolbarActivity
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ResourceUtil

class PageActionMoreDialog(val callback: PageActionItem.Callback,
                           val currentTab: Tab,
                           val model: PageViewModel) : ExtendedBottomSheetDialogFragment() {

    private var _binding: ViewPageActionOverflowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = ViewPageActionOverflowBinding.inflate(inflater, container, false)

        binding.overflowForward.setOnClickListener {
            dismiss()
            callback.forwardClick()
        }

        binding.customizeToolbar.setOnClickListener {
            dismiss()
            startActivity(CustomizeToolbarActivity.newIntent(requireContext()))
        }

        Prefs.customizeToolbarMenuOrder.forEach {
            val view = ItemCustomizeToolbarMenuBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            view.id = item.hashCode()
            view.text = context?.getString(item.titleResId)
            view.setCompoundDrawablesWithIntrinsicBounds(item.iconResId, 0, 0, 0)
            view.setOnClickListener {
                dismiss()
                item.select(callback)
            }
            binding.overflowList.addView(view)
        }

        binding.overflowForward.visibility = if (currentTab.canGoForward()) VISIBLE else GONE
        binding.customizeToolbar.setBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_22))

        for (i in 1 until binding.overflowList.childCount) {
            val view = binding.overflowList.getChildAt(i) as MaterialTextView
            val pageActionItem = PageActionItem.find(view.id)
            val enabled =
                model.page != null && (!model.shouldLoadAsMobileWeb || (model.shouldLoadAsMobileWeb && pageActionItem.isAvailableOnMobileWeb))
            when (pageActionItem) {
                PageActionItem.ADD_TO_WATCHLIST -> {
                    view.setText(if (model.isWatched) R.string.menu_page_watched else R.string.menu_page_watch)
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

        return binding.root
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
