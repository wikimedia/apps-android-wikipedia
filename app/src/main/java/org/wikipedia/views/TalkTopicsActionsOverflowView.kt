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
import org.wikipedia.R
import org.wikipedia.databinding.ViewTalkTopicsActionsOverflowBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.talk.TalkTopicActivity

class TalkTopicsActionsOverflowView(context: Context) : FrameLayout(context) {

    interface Callback {
        fun markAsReadClick()
        fun subscribeClick()
        fun shareClick()
    }

    private var binding = ViewTalkTopicsActionsOverflowBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var callback: Callback
    private var popupWindowHost: PopupWindow? = null

    fun show(anchorView: View, threadItem: ThreadItem, callback: Callback) {
        this.callback = callback
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            PopupWindowCompat.showAsDropDown(it, anchorView, 0, 0, Gravity.END)
        }

        binding.overflowMarkAsRead.text = context.getString(if (threadItem.seen)
            R.string.talk_list_item_overflow_mark_as_unread else R.string.notifications_menu_mark_as_read)
        binding.overflowMarkAsRead.setCompoundDrawablesWithIntrinsicBounds(if (threadItem.seen) R.drawable.ic_outline_markunread_24 else R.drawable.ic_outline_drafts_24, 0, 0, 0)

        binding.overflowSubscribe.isVisible = TalkTopicActivity.isSubscribable(threadItem)
        binding.overflowSubscribe.text = context.getString(if (threadItem.subscribed)
            R.string.talk_list_item_overflow_subscribed else R.string.talk_list_item_overflow_subscribe)
        binding.overflowSubscribe.setCompoundDrawablesWithIntrinsicBounds(if (threadItem.subscribed) R.drawable.ic_notifications_active else R.drawable.ic_notifications_black_24dp, 0, 0, 0)

        binding.overflowMarkAsRead.setOnClickListener {
            dismiss()
            callback.markAsReadClick()
        }

        binding.overflowSubscribe.setOnClickListener {
            dismiss()
            callback.subscribeClick()
        }

        binding.overflowShare.setOnClickListener {
            dismiss()
            callback.shareClick()
        }
    }

    fun dismiss() {
        popupWindowHost?.dismiss()
        popupWindowHost = null
    }
}
