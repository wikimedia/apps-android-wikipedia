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
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.databinding.ViewNotificationActionsOverflowBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationLinkHandler
import org.wikipedia.notifications.NotificationListItemContainer
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class NotificationActionsOverflowView(context: Context) : FrameLayout(context) {

    fun interface Callback {
        fun markAsReadClick(container: NotificationListItemContainer, markRead: Boolean)
    }

    private var binding = ViewNotificationActionsOverflowBinding.inflate(LayoutInflater.from(context), this, true)

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
                val category = NotificationCategory.find(container.notification.category)
                val iconColor = ContextCompat.getColor(context, ResourceUtil.getThemedAttributeId(context, category.iconColor))
                setUpViewForLink(binding.overflowViewPrimary, primary, category.iconResId, iconColor, iconColor)
                binding.overflowViewPrimary.visibility = View.VISIBLE
            }
            it.links?.secondary?.let { secondary ->
                if (secondary.isNotEmpty()) {
                    setUpViewForLink(binding.overflowViewSecondary, secondary.first())
                    binding.overflowViewSecondary.visibility = View.VISIBLE

                    val uri = Uri.parse(secondary.first().url)
                    val pageTitle = WikiSite(uri).titleForUri(uri)
                    if (pageTitle.isUserPage) {
                        binding.overflowViewSecondaryTalk.visibility = View.VISIBLE
                        binding.overflowViewSecondaryTalk.text = context.getString(R.string.notifications_menu_user_talk_page, secondary.first().label)
                        binding.overflowViewSecondaryTalk.setOnClickListener {
                            if (UriUtil.isAppSupportedLink(uri)) {
                                context.startActivity(TalkTopicsActivity.newIntent(context, pageTitle, Constants.InvokeSource.NOTIFICATION))
                            } else {
                                linkHandler.onExternalLinkClicked(uri)
                            }
                        }
                    }

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
            dismiss()
            callback.markAsReadClick(container, container.notification?.isUnread == true)
        }
    }

    fun dismiss() {
        popupWindowHost?.dismiss()
        popupWindowHost = null
    }

    private var actionClickListener = OnClickListener {
        val link = it.tag as Notification.Link
        val linkIndex = if (it.id == R.id.overflow_view_primary) NotificationInteractionEvent.ACTION_PRIMARY else if (it.id == R.id.overflow_view_secondary) NotificationInteractionEvent.ACTION_SECONDARY else NotificationInteractionEvent.ACTION_LINK_CLICKED
        val url = link.url
        val notification = container.notification
        if (url.isNotEmpty() && notification != null) {
            NotificationInteractionFunnel(WikipediaApp.instance, notification).logAction(linkIndex, link)
            NotificationInteractionEvent.logAction(notification, linkIndex, link)
            linkHandler.wikiSite = WikiSite(url)
            linkHandler.onUrlClick(url, null, "")
        }
        dismiss()
    }

    private fun setUpViewForLink(textView: TextView, link: Notification.Link,
                                 @DrawableRes customIcon: Int = R.drawable.ic_arrow_forward_black_24dp,
                                 @ColorInt customIconColor: Int = ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color),
                                 @ColorInt customTextColor: Int = ResourceUtil.getThemedColor(context, R.attr.primary_text_color)) {
        textView.text = StringUtil.fromHtml(link.tooltip.ifEmpty { link.label })

        val icon = when (link.icon()) {
            "userAvatar" -> R.drawable.ic_user_avatar
            "changes" -> R.drawable.ic_icon_revision_history_apps
            "speechBubbles", "userSpeechBubble" -> R.drawable.ic_notification_article_talk
            else -> customIcon
        }

        val iconColor = ColorStateList.valueOf(customIconColor)
        val textColor = ColorStateList.valueOf(customTextColor)
        val drawable = AppCompatResources.getDrawable(context, icon)
        drawable?.setTintList(iconColor)
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        textView.setTextColor(textColor)
        textView.tag = link
        textView.visibility = View.VISIBLE
    }
}
