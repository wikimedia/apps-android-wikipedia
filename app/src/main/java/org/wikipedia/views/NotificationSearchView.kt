package org.wikipedia.views

import android.content.Context
import android.text.Spannable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.wikipedia.databinding.ViewNotificationSearchViewBinding

class NotificationSearchView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    interface Callback {
        fun onSearchEmptyViewVisible(visibility: Int)
    }

    private val binding = ViewNotificationSearchViewBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    init {
        orientation = VERTICAL
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        callback?.onSearchEmptyViewVisible(visibility)
    }

    fun setText(spannedEmptySearchMessage: Spannable) {
        binding.notificationsEmptySearchMessage.setText(spannedEmptySearchMessage, TextView.BufferType.SPANNABLE)
    }
}
