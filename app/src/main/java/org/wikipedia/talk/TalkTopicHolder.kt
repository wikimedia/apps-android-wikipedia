package org.wikipedia.talk

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.Namespace
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.*
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import org.wikipedia.views.TalkTopicsActionsOverflowView
import java.util.*

class TalkTopicHolder internal constructor(
        private val binding: ItemTalkTopicBinding,
        private val context: Context,
        private val viewModel: TalkTopicsViewModel,
        private val invokeSource: Constants.InvokeSource
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, SwipeableItemTouchHelperCallback.Callback {

    private lateinit var threadItem: ThreadItem

    fun bindItem(item: ThreadItem) {
        item.seen = viewModel.topicSeen(item)
        threadItem = item
        binding.topicTitleText.text = RichTextUtil.stripHtml(threadItem.html).trim().ifEmpty { context.getString(R.string.talk_no_subject) }
        binding.topicTitleText.setTextColor(ResourceUtil.getThemedColor(context, if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.material_theme_primary_color))
        StringUtil.highlightAndBoldenText(binding.topicTitleText, viewModel.currentSearchQuery, true, Color.YELLOW)
        itemView.setOnClickListener(this)

        // setting tag for swipe action text
        if (!threadItem.seen) {
            itemView.setTag(R.string.tag_text_key, context.getString(R.string.talk_list_item_swipe_mark_as_read))
            itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_drafts_24)
        } else {
            itemView.setTag(R.string.tag_text_key, context.getString(R.string.talk_list_item_swipe_mark_as_unread))
            itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_email_24)
        }

        binding.topicOverflowMenu.setOnClickListener {
            showOverflowMenu(it)
        }

        val allReplies = threadItem.allReplies

        if (allReplies.isEmpty()) {
            binding.topicUserIcon.isVisible = false
            binding.topicUsername.isVisible = false
            binding.topicReplyIcon.isVisible = false
            binding.topicReplyNumber.isVisible = false
            binding.topicLastCommentDate.isVisible = false
            binding.topicContentText.isVisible = false
            val isHeaderTemplate = TalkTopicActivity.isHeaderTemplate(threadItem)
            binding.otherContentText.isVisible = isHeaderTemplate
            binding.topicOverflowMenu.isVisible = !isHeaderTemplate
            binding.topicTitleText.isVisible = !isHeaderTemplate
            if (isHeaderTemplate) {
                binding.otherContentText.text = RichTextUtil.stripHtml(StringUtil.removeStyleTags(threadItem.othercontent)).trim().replace("\n", " ")
                StringUtil.highlightAndBoldenText(binding.otherContentText, viewModel.currentSearchQuery, true, Color.YELLOW)
            }
            return
        }
        binding.topicTitleText.isVisible = true
        binding.otherContentText.isVisible = false

        // Last comment
        binding.topicContentText.isVisible = viewModel.pageTitle.namespace() == Namespace.USER_TALK
        binding.topicContentText.text = RichTextUtil.stripHtml(allReplies.last().html).trim().replace("\n", " ")
        binding.topicContentText.setTextColor(ResourceUtil.getThemedColor(context, if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.primary_text_color))
        StringUtil.highlightAndBoldenText(binding.topicContentText, viewModel.currentSearchQuery, true, Color.YELLOW)

        // Username with involved user number exclude the author
        val usersInvolved = allReplies.map { it.author }.distinct().size - 1
        val usernameText = allReplies.maxByOrNull { it.date ?: Date() }?.author.orEmpty() + (if (usersInvolved > 1) " +$usersInvolved" else "")
        val usernameColor = if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.colorAccent
        binding.topicUsername.text = usernameText
        binding.topicUserIcon.isVisible = viewModel.pageTitle.namespace() == Namespace.USER_TALK
        binding.topicUsername.isVisible = viewModel.pageTitle.namespace() == Namespace.USER_TALK
        binding.topicUsername.setTextColor(ResourceUtil.getThemedColor(context, usernameColor))
        ImageViewCompat.setImageTintList(binding.topicUserIcon, ResourceUtil.getThemedColorStateList(context, usernameColor))
        StringUtil.highlightAndBoldenText(binding.topicUsername, viewModel.currentSearchQuery, true, Color.YELLOW)

        // Amount of replies, exclude the topic in replies[].
        val replyNumber = allReplies.size - 1
        val replyNumberColor = if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.primary_text_color
        binding.topicReplyNumber.isVisible = replyNumber > 0
        binding.topicReplyIcon.isVisible = replyNumber > 0
        binding.topicReplyNumber.text = replyNumber.toString()
        binding.topicReplyNumber.setTextColor(ResourceUtil.getThemedColor(context, replyNumberColor))
        ImageViewCompat.setImageTintList(binding.topicReplyIcon, ResourceUtil.getThemedColorStateList(context, replyNumberColor))

        // Last comment date
        val lastCommentDate = allReplies.mapNotNull { it.date }.maxOrNull()?.run { DateUtil.getDateAndTime(context, this) }
        val lastCommentColor = if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.secondary_text_color
        binding.topicLastCommentDate.text = context.getString(R.string.talk_list_item_last_comment_date, lastCommentDate)
        binding.topicLastCommentDate.isVisible = lastCommentDate != null
        binding.topicLastCommentDate.setTextColor(ResourceUtil.getThemedColor(context, lastCommentColor))
    }

    override fun onClick(v: View?) {
        markAsSeen(true)
        context.startActivity(TalkTopicActivity.newIntent(context, viewModel.pageTitle, threadItem.name, threadItem.id, null, viewModel.currentSearchQuery, invokeSource))
    }

    override fun onSwipe() {
        markAsSeen()
    }

    override fun isSwipeable(): Boolean {
        return !TalkTopicActivity.isHeaderTemplate(threadItem)
    }

    private fun markAsSeen(force: Boolean = false) {
        viewModel.markAsSeen(threadItem, force)
        bindingAdapter?.notifyDataSetChanged()
    }

    private fun showOverflowMenu(anchorView: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val subscribed = withContext(Dispatchers.IO) {
                viewModel.isSubscribed(threadItem.name)
            }
            threadItem.subscribed = subscribed
            TalkTopicsActionsOverflowView(context).show(anchorView, threadItem, object : TalkTopicsActionsOverflowView.Callback {
                override fun markAsReadClick() {
                    markAsSeen()
                }

                override fun subscribeClick() {
                    viewModel.subscribeTopic(threadItem.name, subscribed)
                    FeedbackUtil.showMessage(context as Activity, context.getString(if (!subscribed) R.string.talk_thread_subscribed_to else R.string.talk_thread_unsubscribed_from,
                        StringUtil.fromHtml(threadItem.html).trim().ifEmpty { context.getString(R.string.talk_no_subject) }))
                }

                override fun shareClick() {
                    ShareUtil.shareText(context, context.getString(R.string.talk_share_discussion_subject,
                        threadItem.html.ifEmpty { context.getString(R.string.talk_no_subject) }), viewModel.pageTitle.uri + "#" + StringUtil.addUnderscores(threadItem.html))
                }
            })
        }
    }
}
