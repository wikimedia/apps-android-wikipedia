package org.wikipedia.talk

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
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
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.*
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import org.wikipedia.views.TalkTopicsActionsOverflowView

class TalkTopicHolder internal constructor(
        private val binding: ItemTalkTopicBinding,
        private val context: Context,
        private val pageTitle: PageTitle,
        private val viewModel: TalkTopicsViewModel,
        private val invokeSource: Constants.InvokeSource
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, SwipeableItemTouchHelperCallback.Callback {

    private lateinit var threadItem: ThreadItem
    private var itemPosition = -1

    fun bindItem(item: ThreadItem, position: Int) {
        item.seen = viewModel.topicSeen(item)
        threadItem = item
        itemPosition = position
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

        val allReplies = threadItem.allReplies

        if (allReplies.isEmpty()) {
            binding.topicBodyGroup.isVisible = false
            binding.topicContentText.isVisible = false
            return
        }

        // Last comment
        binding.topicContentText.isVisible = pageTitle.namespace() == Namespace.USER_TALK
        binding.topicContentText.text = RichTextUtil.stripHtml(allReplies.last().html).trim()
        binding.topicContentText.setTextColor(ResourceUtil.getThemedColor(context, if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.primary_text_color))
        StringUtil.highlightAndBoldenText(binding.topicContentText, viewModel.currentSearchQuery, true, Color.YELLOW)

        // Username with involved user number exclude the author
        val usersInvolved = allReplies.map { it.author }.distinct().size - 1
        val usernameText = allReplies.first().author + (if (usersInvolved > 1) " +$usersInvolved" else "")
        binding.topicUsername.text = usernameText
        StringUtil.highlightAndBoldenText(binding.topicUsername, viewModel.currentSearchQuery, true, Color.YELLOW)

        // Amount of replies, exclude the topic in replies[].
        binding.topicReplyNumber.text = (allReplies.size - 1).toString()

        // Last comment date
        val lastCommentDate = allReplies.mapNotNull { it.date }.maxByOrNull { it }?.run { DateUtil.getDateAndTime(this) }
        binding.topicLastCommentDate.text = context.getString(R.string.talk_list_item_last_comment_date, lastCommentDate)
        binding.topicLastCommentDate.isVisible = lastCommentDate != null

        // Overflow menu
        binding.topicOverflowMenu.setOnClickListener {
            showOverflowMenu(it)
        }
    }

    override fun onClick(v: View?) {
        markAsSeen(true)
        context.startActivity(TalkTopicActivity.newIntent(context, pageTitle, threadItem.name, invokeSource))
    }

    override fun onSwipe() {
        markAsSeen()
    }

    private fun markAsSeen(force: Boolean = false) {
        viewModel.markAsSeen(threadItem, force)
        bindingAdapter?.notifyItemChanged(itemPosition)
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
                        StringUtil.fromHtml(threadItem.html).trim().ifEmpty { context.getString(R.string.talk_no_subject) }), FeedbackUtil.LENGTH_DEFAULT)
                }

                override fun shareClick() {
                    ShareUtil.shareText(context, context.getString(R.string.talk_share_discussion_subject,
                        threadItem.html.ifEmpty { context.getString(R.string.talk_no_subject) }), pageTitle.uri + "#" + StringUtil.addUnderscores(threadItem.html))
                }
            })
        }
    }
}
