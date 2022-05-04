package org.wikipedia.talk

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
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
        item.seen = viewModel.topicSeen(item.id)
        threadItem = item
        itemPosition = position
        binding.topicTitleText.text = RichTextUtil.stripHtml(threadItem.html).trim().ifEmpty { context.getString(R.string.talk_no_subject) }
        binding.topicTitleText.setTextColor(ResourceUtil.getThemedColor(context, if (threadItem.seen) android.R.attr.textColorTertiary else R.attr.material_theme_primary_color))
        StringUtil.highlightAndBoldenText(binding.topicTitleText, viewModel.currentSearchQuery, true, Color.YELLOW)
        itemView.setOnClickListener(this)

        // setting tag for swipe action text
        if (threadItem.seen) {
            itemView.setTag(R.string.tag_text_key, context.getString(R.string.talk_list_item_swipe_mark_as_read))
            itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_drafts_24)
        } else {
            itemView.setTag(R.string.tag_text_key, context.getString(R.string.talk_list_item_swipe_mark_as_unread))
            itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_email_24)
        }

        val allReplies = threadItem.allReplies

        if (allReplies.isEmpty()) {
            binding.topicBodyGroup.isVisible = false
            return
        }

        // Last comment
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
        val lastCommentDate = allReplies.mapNotNull { it.date }.maxByOrNull { it }
        binding.topicLastCommentDate.text = context.getString(R.string.talk_list_item_last_comment_date, lastCommentDate)
        binding.topicLastCommentDate.isVisible = lastCommentDate != null

        // Overflow menu
        binding.topicOverflowMenu.setOnClickListener {
            showOverflowMenu(it)
        }
    }

    override fun onClick(v: View?) {
        markAsSeen()
        context.startActivity(TalkTopicActivity.newIntent(context, pageTitle, threadItem.id, invokeSource))
    }

    override fun onSwipe() {
        markAsSeen()
    }

    private fun markAsSeen() {
        viewModel.markAsSeen(threadItem.id)
        bindingAdapter?.notifyItemChanged(itemPosition)
    }

    private fun showOverflowMenu(anchorView: View) {
        TalkTopicsActionsOverflowView(context).show(anchorView, threadItem, object : TalkTopicsActionsOverflowView.Callback {
            override fun markAsReadClick(threadItem: ThreadItem, markRead: Boolean) {
                markAsSeen()
            }

            override fun subscribeClick(threadItem: ThreadItem, subscribed: Boolean) {
                // TODO: implement this
            }
        })
    }
}
