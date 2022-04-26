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
    }

    var callback: Callback? = null
    private val binding = ItemTalkThreadItemBinding.inflate(LayoutInflater.from(context), this)
    private lateinit var item: ThreadItem

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.replyButton.setOnClickListener {
            // TODO
        }

        binding.showRepliesContainer.setOnClickListener {
            callback?.onExpandClick(item)
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

        binding.topDivider.isVisible = item.level == 1

        binding.showRepliesContainer.isVisible = item.replies.isNotEmpty()
        binding.threadLine.isVisible = item.replies.isNotEmpty()
    }
}
