package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.bundleOf
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.NotificationFunnel
import org.wikipedia.databinding.ViewNotificationActionsBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.GsonUtil
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class NotificationItemActionsDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onArchive(notification: Notification)
        fun onActionPageTitle(pageTitle: PageTitle)
        val isShowingArchived: Boolean
    }

    private var _binding: ViewNotificationActionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notification: Notification
    private lateinit var linkHandler: NotificationLinkHandler

    private var actionClickListener = View.OnClickListener {
        val link = it.tag as Notification.Link
        val linkIndex = if (it.id == R.id.notification_action_primary) 0 else if (it.id == R.id.notification_action_secondary) 1 else 2
        val url = link.url
        if (url.isNotEmpty()) {
            NotificationFunnel(WikipediaApp.getInstance(), notification).logAction(linkIndex, link)
            linkHandler.wikiSite = WikiSite(url)
            linkHandler.onUrlClick(url, null, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ViewNotificationActionsBinding.inflate(inflater, container, false)

        notification = GsonUtil.getDefaultGson().fromJson(requireArguments().getString(ARG_NOTIFICATION), Notification::class.java)
        linkHandler = NotificationLinkHandler(requireContext())
        notification.contents?.let {
            binding.notificationItemText.text = StringUtil.fromHtml(it.header).toString()
            it.links?.primary?.let { primary ->
                setUpViewForLink(binding.notificationActionPrimary, binding.notificationActionPrimaryIcon, binding.notificationActionPrimaryText, primary)
                binding.notificationActionPrimary.visibility = View.VISIBLE
            }
            it.links?.secondary?.let { secondary ->
                if (secondary.size > 0) {
                    setUpViewForLink(binding.notificationActionSecondary, binding.notificationActionSecondaryIcon, binding.notificationActionSecondaryText, secondary[0])
                    binding.notificationActionSecondary.visibility = View.VISIBLE
                    if (secondary.size > 1) {
                        setUpViewForLink(binding.notificationActionTertiary, binding.notificationActionTertiaryIcon, binding.notificationActionTertiaryText, secondary[1])
                        binding.notificationActionTertiary.visibility = View.VISIBLE
                    }
                }
            }
        }

        callback()?.let {
            binding.notificationItemArchiveIcon.setImageResource(if (it.isShowingArchived) R.drawable.ic_unarchive_themed_24dp else R.drawable.ic_archive_themed_24dp)
            binding.notificationItemArchiveText.setText(if (it.isShowingArchived) R.string.notifications_mark_unread else R.string.notifications_archive)
        }

        binding.notificationItemArchive.setOnClickListener {
            callback()?.onArchive(notification)
        }

        binding.notificationActionPrimary.setOnClickListener(actionClickListener)
        binding.notificationActionSecondary.setOnClickListener(actionClickListener)
        binding.notificationActionTertiary.setOnClickListener(actionClickListener)

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setUpViewForLink(containerView: View, iconView: AppCompatImageView, labelView: TextView, link: Notification.Link) {
        if (link.tooltip.isNotEmpty()) {
            labelView.text = StringUtil.fromHtml(link.tooltip)
        } else {
            labelView.text = StringUtil.fromHtml(link.label)
        }
        if ("userAvatar" == link.icon) {
            iconView.setImageResource(R.drawable.ic_user_avatar)
        } else {
            iconView.setImageResource(R.drawable.ic_arrow_forward_black_24dp)
        }
        containerView.tag = link
        containerView.visibility = View.VISIBLE
    }

    private inner class NotificationLinkHandler constructor(context: Context) : LinkHandler(context) {

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // ignore
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // ignore
        }

        override lateinit var wikiSite: WikiSite

        override fun onInternalLinkClicked(title: PageTitle) {
            callback()?.onActionPageTitle(title)
        }

        override fun onExternalLinkClicked(uri: Uri) {
            try {
                // TODO: handle "change password" since it will open a blank page in PageActivity
                startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_NOTIFICATION = "notification"

        fun newInstance(notification: Notification): NotificationItemActionsDialog {
            return NotificationItemActionsDialog().apply {
                arguments = bundleOf(ARG_NOTIFICATION to GsonUtil.getDefaultGson().toJson(notification))
            }
        }
    }
}
