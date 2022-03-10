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
import kotlinx.coroutines.*
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryFilterOverflowBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

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

    fun show(anchorView: View, pageTitle: PageTitle, callback: Callback?) {
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

        binding.filterByAllSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_EDITS)
        binding.filterByUserSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_EDITORS)
        binding.filterByAnonSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_ANONYMOUS)
        binding.filterByBotSelected.isVisible = Prefs.editHistoryFilterSet.contains(EditCount.EDIT_TYPE_BOT)

        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, msg -> run { L.e(msg) } }) {
            withContext(Dispatchers.IO) {
                val editCountsAllResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITS) }
                val editCountsUserResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITORS) }
                val editCountsAnonResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_ANONYMOUS) }
                val editCountsBotResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_BOT) }

                setUpFilterItem(binding.filterByAll, R.string.page_edit_history_filter_by_all, editCountsAllResponse)
                setUpFilterItem(binding.filterByUser, R.string.page_edit_history_filter_by_user, editCountsUserResponse)
                setUpFilterItem(binding.filterByAnon, R.string.page_edit_history_filter_by_anon, editCountsAnonResponse)
                setUpFilterItem(binding.filterByBot, R.string.page_edit_history_filter_by_bot, editCountsBotResponse)
            }
        }
    }

    suspend fun setUpFilterItem(view: TextView, @StringRes stringId: Int, response: Deferred<EditCount>) {
        view.text = context.getString(stringId, StringUtil.getPageViewText(context, response.await().count.toLong()))
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
