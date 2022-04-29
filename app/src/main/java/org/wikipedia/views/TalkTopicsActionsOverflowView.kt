package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.PopupWindowCompat
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.databinding.ViewNotificationActionsOverflowBinding
import org.wikipedia.databinding.ViewTalkTopicsActionsOverflowBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationLinkHandler
import org.wikipedia.notifications.NotificationListItemContainer
import org.wikipedia.notifications.db.Notification
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.TalkTopicsViewModel
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class TalkTopicsActionsOverflowView(context: Context) : FrameLayout(context) {

    interface Callback {
        fun markAsReadClick(threadItem: ThreadItem, markRead: Boolean)
        fun subscribeClick(threadItem: ThreadItem, subscribed: Boolean)
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

        // TODO: update read and subscribe status

        binding.overflowMarkAsRead.setOnClickListener {
            dismiss()
            callback.markAsReadClick(threadItem, true)
        }

        binding.overflowSubscribe.setOnClickListener {
            dismiss()
            // TODO: use actual subscribe status
            callback.subscribeClick(threadItem, true)
        }

        binding.overflowShare.setOnClickListener {
            dismiss()
            // TODO: implement share
        }
    }

    fun dismiss() {
        popupWindowHost?.dismiss()
        popupWindowHost = null
    }
}
