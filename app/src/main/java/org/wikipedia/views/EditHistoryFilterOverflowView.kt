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
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryFilterOverflowBinding
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.page.edithistory.EditHistoryListViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

class EditHistoryFilterOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun onItemClicked()
    }

    private var binding = ViewEditHistoryFilterOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, editCounts: EditHistoryListViewModel.EditHistoryStats, callback: Callback?) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }
        popupWindowHost?.setOnDismissListener {
            popupWindowHost = null
        }

        binding.filterByAll.text = context.getString(R.string.page_edit_history_filter_by_all, StringUtil.getPageViewText(context, editCounts.allEdits.count.toLong()))
        binding.filterByUser.text = context.getString(R.string.page_edit_history_filter_by_user, StringUtil.getPageViewText(context, editCounts.userEdits.count.toLong()))
        binding.filterByAnon.text = context.getString(R.string.page_edit_history_filter_by_anon, StringUtil.getPageViewText(context, editCounts.anonEdits.count.toLong()))
        binding.filterByBot.text = context.getString(R.string.page_edit_history_filter_by_bot, StringUtil.getPageViewText(context, editCounts.botEdits.count.toLong()))
        updateSelectedItem()
    }

    private fun updateSelectedItem() {
        binding.filterByAllSelected.visibility = View.INVISIBLE
        binding.filterByAnonSelected.visibility = View.INVISIBLE
        binding.filterByUserSelected.visibility = View.INVISIBLE
        binding.filterByBotSelected.visibility = View.INVISIBLE

        when (Prefs.editHistoryFilterType) {
            EditCount.EDIT_TYPE_ANONYMOUS -> { binding.filterByAnonSelected.visibility = View.VISIBLE }
            EditCount.EDIT_TYPE_EDITORS -> { binding.filterByUserSelected.visibility = View.VISIBLE }
            else -> { binding.filterByAllSelected.visibility = View.VISIBLE }
        }
    }

    private fun setButtonsListener() {
        binding.filterByAllButton.setOnClickListener {
            Prefs.editHistoryFilterType = ""
            onSelected()
        }
        binding.filterByUserButton.setOnClickListener {
            Prefs.editHistoryFilterType = EditCount.EDIT_TYPE_EDITORS
            onSelected()
        }
        binding.filterByAnonButton.setOnClickListener {
            Prefs.editHistoryFilterType = EditCount.EDIT_TYPE_ANONYMOUS
            onSelected()
        }
    }

    private fun onSelected() {
        callback?.onItemClicked()
        popupWindowHost?.dismiss()
    }
}
