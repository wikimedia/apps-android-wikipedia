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
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.databinding.ViewNotificationItemOverflowBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.NotificationLinkHandler
import org.wikipedia.notifications.NotificationListItemContainer
import org.wikipedia.util.StringUtil

class NotificationItemOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun markAsReadClick(container: NotificationListItemContainer, markRead: Boolean)
    }

    private var binding = ViewNotificationItemOverflowBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var container: NotificationListItemContainer
    private lateinit var linkHandler: NotificationLinkHandler
    private lateinit var callback: Callback
    private var popupWindowHost: PopupWindow? = null

    fun show(anchorView: View, container: NotificationListItemContainer, callback: Callback) {
        this.callback = callback
        this.container = container
        this.linkHandler = NotificationLinkHandler(anchorView.context)
        popupWindowHost = PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindowHost?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            PopupWindowCompat.setOverlapAnchor(it, true)
            PopupWindowCompat.showAsDropDown(it, anchorView, 0, 0, Gravity.END)
        }


        container.notification?.contents?.let {
            it.links?.getPrimary()?.let { primary ->
                setUpViewForLink(binding.overflowViewPrimary, primary, true)
                binding.overflowViewPrimary.visibility = View.VISIBLE
            }
            it.links?.secondary?.let { secondary ->
                if (secondary.isNotEmpty()) {
                    setUpViewForLink(binding.overflowViewSecondary, secondary.first())
                    binding.overflowViewSecondary.visibility = View.VISIBLE
                    if (secondary.size > 1) {
                        setUpViewForLink(binding.overflowViewTertiary, secondary[1])
                        binding.overflowViewTertiary.visibility = View.VISIBLE
                    }
                }
            }
        }

        container.notification?.isUnread?.let {
            binding.overflowMarkAsRead.setText(if (it) R.string.notifications_menu_mark_as_read else R.string.notifications_menu_mark_as_unread)
            binding.overflowMarkAsRead.setCompoundDrawablesWithIntrinsicBounds(if (it) R.drawable.ic_outline_markunread_24 else R.drawable.ic_outline_drafts_24, 0, 0, 0)
        }

        binding.overflowViewPrimary.setOnClickListener(actionClickListener)
        binding.overflowViewSecondary.setOnClickListener(actionClickListener)
        binding.overflowViewTertiary.setOnClickListener(actionClickListener)
        binding.overflowMarkAsRead.setOnClickListener {
            dismissPopupWindowHost()
            callback.markAsReadClick(container, container.notification?.isUnread == true)
        }
    }

    private var actionClickListener = View.OnClickListener {
        val link = it.tag as Notification.Link
        val linkIndex = if (it.id == R.id.notification_action_primary) NotificationInteractionEvent.ACTION_PRIMARY else if (it.id == R.id.notification_action_secondary) NotificationInteractionEvent.ACTION_SECONDARY else NotificationInteractionEvent.ACTION_LINK_CLICKED
        val url = link.url
        val notification = container.notification
        if (url.isNotEmpty() && notification != null) {
            NotificationInteractionFunnel(WikipediaApp.getInstance(), notification).logAction(linkIndex, link)
            NotificationInteractionEvent.logAction(notification, linkIndex, link)
            linkHandler.wikiSite = WikiSite(url)
            linkHandler.onUrlClick(url, null, "")
        }
        dismissPopupWindowHost()
    }

    private fun setUpViewForLink(textView: MaterialTextView, link: Notification.Link, useDefaultIcon: Boolean = false) {
        textView.text = StringUtil.fromHtml(link.tooltip.ifEmpty { link.label })

        if (!useDefaultIcon) {
            val icon = when (link.icon) {
                "userAvatar" -> R.drawable.ic_user_avatar
                "changes" -> R.drawable.ic_icon_revision_history_apps
                "edit-user-talk" -> R.drawable.ic_user_talk
                else -> R.drawable.ic_arrow_forward_black_24dp
            }

            textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        }
        textView.tag = link
        textView.visibility = View.VISIBLE
    }

    private fun dismissPopupWindowHost() {
        popupWindowHost?.let {
            it.dismiss()
            popupWindowHost = null
        }
    }
}
