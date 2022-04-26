package org.wikipedia.talk

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkThreadItemBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class TalkThreadItemView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onExpandClick(item: ThreadItem)
        fun onReplyClick(item: ThreadItem)
    }

    var callback: Callback? = null
    private val binding = ItemTalkThreadItemBinding.inflate(LayoutInflater.from(context), this)
    private lateinit var item: ThreadItem

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.replyButton.setOnClickListener {
            callback?.onReplyClick(item)
        }

        binding.showRepliesContainer.setOnClickListener {
            callback?.onExpandClick(item)
            updateExpandedState()
        }
    }

    fun bindItem(item: ThreadItem, movementMethod: MovementMethod) {
        this.item = item
        binding.userNameText.text = item.author
        binding.timeStampText.text = item.timestamp
        binding.bodyText.text = StringUtil.fromHtml(item.html)
        binding.bodyText.movementMethod = movementMethod

        if (item.level > 1) {
            binding.replyButton.backgroundTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.color_group_22))
            binding.replyButton.iconTint = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
            binding.replyButton.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
        } else {
            binding.replyButton.backgroundTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
            binding.replyButton.iconTint = ColorStateList.valueOf(Color.WHITE)
            binding.replyButton.setTextColor(Color.WHITE)
        }

        binding.topDivider.isVisible = item.level <= 2
        binding.threadLineTop.isVisible = item.level > 2
        binding.showRepliesContainer.isVisible = item.level > 1 && item.replies.isNotEmpty()
        binding.threadLineMiddle.isVisible = item.level > 1 && (item.replies.isNotEmpty() || (item.level > 2 && !item.isLastSibling))
        updateExpandedState()
    }

    private fun updateExpandedState() {
        binding.showRepliesArrow.setImageResource(if (item.isExpanded) R.drawable.ic_arrow_drop_down_black_24dp else R.drawable.ic_arrow_forward_24)
        binding.showRepliesText.text = context.resources.getQuantityString(if (item.isExpanded) R.plurals.talk_hide_replies_count else R.plurals.talk_show_replies_count, item.replies.size, item.replies.size)
        binding.threadLineBottom.isVisible = item.isExpanded || (item.level > 2 && !item.isLastSibling)
    }
}
