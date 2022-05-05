package org.wikipedia.talk

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkThreadItemBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.util.*

@SuppressLint("RestrictedApi")
class TalkThreadItemView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onExpandClick(item: ThreadItem)
        fun onReplyClick(item: ThreadItem)
        fun onShareClick(item: ThreadItem)
        fun onUserNameClick(item: ThreadItem, view: View)
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

        binding.overflowButton.setOnClickListener {
            val builder = MenuBuilder(context)
            MenuInflater(context).inflate(R.menu.menu_talk_thread_item, builder)
            builder.setCallback(overflowMenuListener)
            val helper = MenuPopupHelper(context, builder, binding.overflowButton)
            helper.setForceShowIcon(true)
            helper.gravity = Gravity.END
            helper.show()
        }

        binding.userNameText.setOnClickListener {
            callback?.onUserNameClick(item, it)
        }
    }

    fun bindItem(item: ThreadItem, movementMethod: MovementMethod, replying: Boolean = false) {
        this.item = item
        binding.userNameText.text = item.author
        binding.timeStampText.isVisible = item.date != null
        item.date?.let { binding.timeStampText.text = DateUtil.getDateAndTimeWithPipe(it) }
        binding.bodyText.text = StringUtil.fromHtml(item.html).trim()
        binding.bodyText.movementMethod = movementMethod

        if (replying) {
            binding.replyButton.isVisible = false
            binding.topDivider.isVisible = false
            binding.threadLineTop.isVisible = false
            binding.showRepliesContainer.isVisible = false
            binding.threadLineMiddle.isVisible = false
            binding.threadLineBottom.isVisible = false
            return
        }

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

    fun animateSelectedBackground() {
        val colorFrom = ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color)
        val colorTo = ResourceUtil.getThemedColor(context, R.attr.paper_color)
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        anim.duration = 1000
        anim.addUpdateListener {
            if (it.isRunning) {
                setBackgroundColor(it.animatedValue as Int)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }
        anim.start()
    }

    private fun updateExpandedState() {
        binding.showRepliesArrow.setImageResource(if (item.isExpanded) R.drawable.ic_arrow_drop_down_black_24dp else R.drawable.ic_arrow_forward_24)
        binding.showRepliesText.text = context.resources.getQuantityString(if (item.isExpanded) R.plurals.talk_hide_replies_count else R.plurals.talk_show_replies_count, item.replies.size, item.replies.size)
        binding.threadLineBottom.isVisible = item.isExpanded || (item.level > 2 && !item.isLastSibling)
    }

    private val overflowMenuListener = object : MenuBuilder.Callback {
        override fun onMenuItemSelected(menu: MenuBuilder, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_talk_topic_share -> {
                    callback?.onShareClick(item)
                    true
                }
                R.id.menu_copy_text -> {
                    ClipboardUtil.setPlainText(context, null, StringUtil.fromHtml(item.html))
                    FeedbackUtil.showMessage(context as Activity, R.string.text_copied)
                    true
                }
                else -> false
            }
        }

        override fun onMenuModeChange(menu: MenuBuilder) { }
    }
}
