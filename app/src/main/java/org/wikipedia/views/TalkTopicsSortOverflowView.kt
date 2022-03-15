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
import org.wikipedia.databinding.ViewTalkTopicsSortOverflowBinding

class TalkTopicsSortOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun sortByClicked(sortByMode: Int)
    }

    private var binding = ViewTalkTopicsSortOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null
    private var currentSortMode = SORT_BY_DATE_PUBLISHED_DESCENDING

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, sortMode: Int, callback: Callback?) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }

        currentSortMode = sortMode

        when (sortMode) {
            SORT_BY_DATE_PUBLISHED_DESCENDING -> {
                binding.sortByDatePublishedSelected.isVisible = true
                binding.sortByDatePublishedOrder.isVisible = true
                binding.sortByDatePublishedOrder.rotation = 90f
            }
            SORT_BY_DATE_PUBLISHED_ASCENDING -> {
                binding.sortByDatePublishedSelected.isVisible = true
                binding.sortByDatePublishedOrder.isVisible = true
                binding.sortByDatePublishedOrder.rotation = 270f
            }
            SORT_BY_TOPIC_NAME_DESCENDING -> {
                binding.sortByTopicNameSelected.isVisible = true
                binding.sortByTopicNameOrder.isVisible = true
                binding.sortByTopicNameOrder.rotation = 90f
            }
            SORT_BY_TOPIC_NAME_ASCENDING -> {
                binding.sortByTopicNameSelected.isVisible = true
                binding.sortByTopicNameOrder.isVisible = true
                binding.sortByTopicNameOrder.rotation = 270f
            }
        }
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }

    private fun setButtonsListener() {
        binding.sortByDatePublishedButton.setOnClickListener {
            dismissPopupWindowHost()
            callback?.sortByClicked(getNewSortByMode(true))
        }
        binding.sortByTopicNameButton.setOnClickListener {
            dismissPopupWindowHost()
            callback?.sortByClicked(getNewSortByMode(false))
        }
    }

    private fun getNewSortByMode(isDatePublishedClicked: Boolean): Int {
        return if (isDatePublishedClicked) {
            if (currentSortMode == SORT_BY_DATE_PUBLISHED_DESCENDING) SORT_BY_DATE_PUBLISHED_ASCENDING else SORT_BY_DATE_PUBLISHED_DESCENDING
        } else {
            if (currentSortMode == SORT_BY_TOPIC_NAME_DESCENDING) SORT_BY_TOPIC_NAME_ASCENDING else SORT_BY_TOPIC_NAME_DESCENDING
        }
    }

    companion object {
        const val SORT_BY_DATE_PUBLISHED_DESCENDING = 0
        const val SORT_BY_DATE_PUBLISHED_ASCENDING = 1
        const val SORT_BY_TOPIC_NAME_DESCENDING = 2
        const val SORT_BY_TOPIC_NAME_ASCENDING = 3
    }
}
