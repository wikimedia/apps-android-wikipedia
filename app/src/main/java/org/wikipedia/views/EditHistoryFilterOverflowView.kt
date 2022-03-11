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
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryFilterOverflowBinding
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.page.edithistory.EditHistoryListViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil

class EditHistoryFilterOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun filterByClicked(filterBy: String)
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

        setUpFilterItem(binding.filterByAll, R.string.page_edit_history_filter_by_all, editCounts.allEdits)
        setUpFilterItem(binding.filterByUser, R.string.page_edit_history_filter_by_user, editCounts.userEdits)
        setUpFilterItem(binding.filterByAnon, R.string.page_edit_history_filter_by_anon, editCounts.anonEdits)
        setUpFilterItem(binding.filterByBot, R.string.page_edit_history_filter_by_bot, editCounts.botEdits)

        binding.filterByAllSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_EDITS)
        binding.filterByUserSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_EDITORS)
        binding.filterByAnonSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_ANONYMOUS)
        binding.filterByBotSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_BOT)
    }

    private fun setUpFilterItem(view: TextView, @StringRes stringId: Int, editCount: EditCount) {
        view.text = context.getString(stringId, StringUtil.getPageViewText(context, editCount.count.toLong()))
    }

    private fun setButtonsListener() {
        binding.filterByAllButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_EDITS)
        }
        binding.filterByUserButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_EDITORS)
        }
        binding.filterByAnonButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_ANONYMOUS)
        }
        binding.filterByBotButton.setOnClickListener {
            saveToPreference(EditCount.EDIT_TYPE_BOT)
        }
    }

    private fun saveToPreference(filterBy: String) {
        val set = Prefs.editHistoryFilterSet.toMutableSet()
        set.add(filterBy)
        Prefs.editHistoryFilterSet = set
        callback?.filterByClicked(filterBy)
    }
}
