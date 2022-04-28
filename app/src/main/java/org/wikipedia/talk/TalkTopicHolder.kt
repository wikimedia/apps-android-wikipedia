package org.wikipedia.talk

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
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

class TalkTopicHolder internal constructor(
    private val binding: ItemTalkTopicBinding,
    private val context: Context,
    private val pageTitle: PageTitle,
    private val invokeSource: Constants.InvokeSource
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val unreadTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private var id: String = ""

    fun bindItem(threadItem: ThreadItem, viewModel: TalkTopicsViewModel) {
        id = threadItem.id
        val seen = viewModel.getSeenStatus(id)
        var titleStr = RichTextUtil.stripHtml(threadItem.html).trim()
        if (titleStr.isEmpty()) {
            threadItem.replies.firstOrNull()?.let {
                titleStr = RichTextUtil.stripHtml(it.html).replace("\n", " ")
            }
        }

        binding.topicTitleText.text = titleStr.ifEmpty { context.getString(R.string.talk_no_subject) }
        binding.topicTitleText.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
        binding.topicTitleText.setTextColor(ResourceUtil.getThemedColor(context, if (seen) android.R.attr.textColorTertiary else R.attr.material_theme_primary_color))

        StringUtil.highlightAndBoldenText(binding.topicTitleText, viewModel.currentSearchQuery, true, Color.YELLOW)
        itemView.setOnClickListener(this)

        val allReplies = threadItem.allReplies

        if (allReplies.isEmpty()) {
            binding.topicBodyGroup.isVisible = false
            return
        }

        // Last comment
        binding.topicContentText.text = RichTextUtil.stripHtml(allReplies.last().html).trim()
        binding.topicContentText.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
        binding.topicContentText.setTextColor(ResourceUtil.getThemedColor(context, if (seen) android.R.attr.textColorTertiary else R.attr.primary_text_color))

        // Username with involved user number exclude the author
        val usersInvolved = allReplies.map { it.author }.distinct().size - 1
        val usernameText = allReplies.first().author + (if (usersInvolved > 1) " +${usersInvolved}" else "")
        val usernameColor = if (seen) android.R.attr.textColorTertiary else R.attr.colorAccent
        binding.topicUsername.text = usernameText
        binding.topicUsername.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
        binding.topicUsername.setTextColor(ResourceUtil.getThemedColor(context, usernameColor))
        ImageViewCompat.setImageTintList(binding.topicUserIcon, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, usernameColor)))

        // Amount of replies, exclude the topic in replies[].
        val replyNumberColor = if (seen) android.R.attr.textColorTertiary else R.attr.primary_text_color
        binding.topicReplyNumber.text = (allReplies.size - 1).toString()
        binding.topicReplyNumber.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
        binding.topicReplyNumber.setTextColor(ResourceUtil.getThemedColor(context, replyNumberColor))
        ImageViewCompat.setImageTintList(binding.topicReplyIcon, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, replyNumberColor)))

        // Last comment date
        val lastCommentDate = allReplies.maxByOrNull { it.timestamp }?.timestamp?.run { DateUtil.getDateAndTime(DateUtil.iso8601DateParse(this)) }
        binding.topicLastCommentDate.text = context.getString(R.string.talk_list_item_last_comment_date, lastCommentDate)
        binding.topicLastCommentDate.isVisible = !lastCommentDate.isNullOrEmpty()
    }

    override fun onClick(v: View?) {
        context.startActivity(TalkTopicActivity.newIntent(context, pageTitle, id, invokeSource))
    }

    companion object {
        private const val MAX_CHARS_NO_SUBJECT = 100
    }
}
