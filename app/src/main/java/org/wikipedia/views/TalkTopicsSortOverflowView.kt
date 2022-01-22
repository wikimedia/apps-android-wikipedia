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

    interface Callback {
        fun datePublishedClick(isAscending: Boolean)
        fun topicNameClicked(isAscending: Boolean)
    }

    private var binding = ViewTalkTopicsSortOverflowBinding.inflate(LayoutInflater.from(context), this, true)
    private var callback: Callback? = null
    private var popupWindowHost: PopupWindow? = null
    private var isAscending: Boolean = false

    init {
        setButtonsListener()
    }

    fun show(anchorView: View, sortByMode: Int, callback: Callback?) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            it.showAsDropDown(anchorView, 0, 0, Gravity.END)
        }

        when (sortByMode) {
            SORT_BY_DATE_PUBLISHED_DESCENDING -> {
                binding.sortByDatePublishedSelected.isVisible = true
                binding.sortByDatePublishedOrder.isVisible = true
                binding.sortByDatePublishedOrder.rotation = 90f
                isAscending = false
            }
            SORT_BY_DATE_PUBLISHED_ASCENDING -> {
                binding.sortByDatePublishedSelected.isVisible = true
                binding.sortByDatePublishedOrder.isVisible = true
                binding.sortByDatePublishedOrder.rotation = 270f
                isAscending = true
            }
            SORT_BY_TOPIC_NAME_DESCENDING -> {
                binding.sortByTopicNameSelected.isVisible = true
                binding.sortByTopicNameOrder.isVisible = true
                binding.sortByTopicNameOrder.rotation = 90f
                isAscending = false
            }
            SORT_BY_TOPIC_NAME_ASCENDING -> {
                binding.sortByTopicNameSelected.isVisible = true
                binding.sortByTopicNameOrder.isVisible = true
                binding.sortByTopicNameOrder.rotation = 270f
                isAscending = true
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
            callback?.datePublishedClick(isAscending)
        }
        binding.sortByTopicNameButton.setOnClickListener {
            dismissPopupWindowHost()
            callback?.topicNameClicked(isAscending)
        }
    }

    companion object {
        const val SORT_BY_DATE_PUBLISHED_DESCENDING = 0
        const val SORT_BY_DATE_PUBLISHED_ASCENDING = 1
        const val SORT_BY_TOPIC_NAME_DESCENDING = 2
        const val SORT_BY_TOPIC_NAME_ASCENDING = 3
    }
}
