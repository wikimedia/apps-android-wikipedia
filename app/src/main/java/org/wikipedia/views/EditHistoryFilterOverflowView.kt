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

    fun show(anchorView: View, editCounts: EditHistoryListViewModel.EditHistoryEditCounts, callback: Callback?) {
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
        updateSelectedIconsVisibility()
    }

    private fun updateSelectedIconsVisibility() {
        val icons = listOf(binding.filterByUserSelected, binding.filterByAnonSelected, binding.filterByBotSelected)
        val types = listOf(EditCount.EDIT_TYPE_EDITORS, EditCount.EDIT_TYPE_ANONYMOUS, EditCount.EDIT_TYPE_BOT)
        if (Prefs.editHistoryFilterEnableSet.isEmpty()) {
            icons.map { it.visibility = View.VISIBLE }
            binding.filterByAllSelected.visibility = View.VISIBLE
        } else {
            types.forEachIndexed { index, type ->
                icons[index].visibility = if (Prefs.editHistoryFilterEnableSet.contains(type)) View.VISIBLE else View.INVISIBLE
            }
            binding.filterByAllSelected.visibility = View.INVISIBLE
        }
    }

    private fun setButtonsListener() {
        binding.filterByAllButton.setOnClickListener {
            saveToPreference()
            updateSelectedIconsVisibility()
            callback?.onItemClicked()
            popupWindowHost?.dismiss()
        }
        binding.filterByUserButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_EDITORS)
            updateSelectedIconsVisibility()
            callback?.onItemClicked()
            popupWindowHost?.dismiss()
        }
        binding.filterByAnonButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_ANONYMOUS)
            updateSelectedIconsVisibility()
            callback?.onItemClicked()
            popupWindowHost?.dismiss()
        }
        binding.filterByBotButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_BOT)
            updateSelectedIconsVisibility()
            callback?.onItemClicked()
            popupWindowHost?.dismiss()
        }
    }

    private fun saveToPreference(enableType: String? = null) {
        val set = Prefs.editHistoryFilterEnableSet.toMutableSet()
        set.clear()
        enableType?.let {
            if (set.contains(enableType)) {
                set.remove(enableType)
            } else {
                set.add(enableType)
            }
        }
        Prefs.editHistoryFilterEnableSet = set
    }
}
