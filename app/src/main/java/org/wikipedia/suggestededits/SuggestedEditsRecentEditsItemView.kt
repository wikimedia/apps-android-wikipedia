package org.wikipedia.suggestededits

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemSuggestedEditsRecentEditsBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class SuggestedEditsRecentEditsItemView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    val binding = ItemSuggestedEditsRecentEditsBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: Callback? = null
    private var item: MwQueryResult.RecentChange? = null
    private var clickListener = OnClickListener {
        item?.let {
            callback?.onItemClick(it)
        }
    }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.containerView.setOnClickListener(clickListener)
        binding.diffText.setOnClickListener(clickListener)
        binding.userNameText.setOnClickListener { view ->
            item?.let {
                callback?.onUserClick(it, view)
            }
        }
    }

    @Suppress("KotlinConstantConditions")
    fun setItem(item: MwQueryResult.RecentChange, currentQuery: String?) {
        this.item = item
        var isSummaryEmpty = false
        var isTagsEmpty = false
        binding.titleText.text = item.title
        binding.summaryText.text = StringUtil.fromHtml(item.parsedComment).ifEmpty {
            isSummaryEmpty = true
            context.getString(R.string.page_edit_history_comment_placeholder)
        }
        binding.summaryText.setTypeface(Typeface.SANS_SERIF, if (isSummaryEmpty) Typeface.ITALIC else Typeface.NORMAL)
        binding.summaryText.setTextColor(ResourceUtil.getThemedColor(context,
            if (isSummaryEmpty) R.attr.secondary_color else R.attr.primary_color))

        val tagsString = item.joinedTags.ifEmpty {
            isTagsEmpty = true
            context.getString(R.string.patroller_tasks_edits_list_card_tags_text_none)
        }
        binding.tagsText.text = context.getString(R.string.patroller_tasks_edits_list_card_tags_text, tagsString)
        binding.tagsText.setTypeface(Typeface.SANS_SERIF, if (isTagsEmpty) Typeface.ITALIC else Typeface.NORMAL)
        binding.tagsText.setTextColor(ResourceUtil.getThemedColor(context,
            if (isTagsEmpty) R.attr.secondary_color else R.attr.primary_color))
        binding.timeText.text = DateUtil.getTimeString(context, item.parsedDateTime)
        binding.userNameText.text = item.user
        binding.userNameText.contentDescription = context.getString(R.string.talk_user_title, item.user)

        binding.userNameText.setIconResource(if (item.anon) R.drawable.ic_anonymous_ooui else R.drawable.ic_user_avatar)
        val diffByteCount = item.newlen - item.oldlen
        setButtonTextAndIconColor(StringUtil.getDiffBytesText(context, diffByteCount))
        if (diffByteCount >= 0) {
            val diffColor = if (diffByteCount > 0) R.attr.success_color else R.attr.secondary_color
            binding.diffText.setTextColor(ResourceUtil.getThemedColor(context, diffColor))
        } else {
            binding.diffText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.destructive_color))
        }
        binding.diffText.isVisible = true
        binding.containerView.alpha = 1.0f
        binding.containerView.isClickable = true

        StringUtil.highlightAndBoldenText(binding.titleText, currentQuery, true, Color.YELLOW)
        StringUtil.highlightAndBoldenText(binding.timeText, currentQuery, true, Color.YELLOW)
        StringUtil.highlightAndBoldenText(binding.userNameText, currentQuery, true, Color.YELLOW)
        StringUtil.highlightAndBoldenText(binding.tagsText, currentQuery, true, Color.YELLOW)
    }

    private fun setButtonTextAndIconColor(text: String, @DrawableRes iconResourceDrawable: Int = 0) {
        val themedTint = ResourceUtil.getThemedColorStateList(context, R.attr.border_color)
        binding.diffText.text = text
        binding.diffText.setTextColor(themedTint)
        binding.diffText.setIconResource(iconResourceDrawable)
        binding.diffText.iconTint = themedTint
    }

    interface Callback {
        fun onItemClick(item: MwQueryResult.RecentChange)
        fun onUserClick(item: MwQueryResult.RecentChange, view: View)
    }
}
