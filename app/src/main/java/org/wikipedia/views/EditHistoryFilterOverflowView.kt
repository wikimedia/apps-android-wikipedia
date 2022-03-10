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
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.databinding.ViewEditHistoryFilterOverflowBinding
import org.wikipedia.settings.Prefs

class EditHistoryFilterOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun filterByClicked(filterBy: Int)
    }

    private var binding = ViewEditHistoryFilterOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, callback: Callback?) {
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

        Prefs.editHistoryFilterSet.forEach {
            when (it) {
                FILTER_BY_ALL -> {
                    binding.filterByAllSelected.isVisible = true
                }
                FILTER_BY_USER -> {
                    binding.filterByUserSelected.isVisible = true
                }
                FILTER_BY_ANON -> {
                    binding.filterByAnonSelected.isVisible = true
                }
                FILTER_BY_BOT -> {
                    binding.filterByBotSelected.isVisible = true
                }
            }
        }
    }

    private fun setButtonsListener() {
        binding.filterByAll.setOnClickListener {
            saveToPreference(FILTER_BY_ALL)
        }
        binding.filterByUser.setOnClickListener {
            saveToPreference(FILTER_BY_USER)
        }
        binding.filterByAnon.setOnClickListener {
            saveToPreference(FILTER_BY_ANON)
        }
        binding.filterByBot.setOnClickListener {
            saveToPreference(FILTER_BY_BOT)
        }
    }

    private fun saveToPreference(filterBy: Int) {
        val set = Prefs.editHistoryFilterSet.toMutableSet()
        set.add(filterBy)
        Prefs.editHistoryFilterSet = set
        callback?.filterByClicked(filterBy)
    }

    companion object {
        const val FILTER_BY_ALL = 0
        const val FILTER_BY_USER = 1
        const val FILTER_BY_ANON = 2
        const val FILTER_BY_BOT = 3
    }
}
