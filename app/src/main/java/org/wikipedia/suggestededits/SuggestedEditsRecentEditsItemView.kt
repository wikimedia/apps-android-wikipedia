package org.wikipedia.suggestededits

import android.content.Context
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
        if (item != null) {
            callback?.onItemClick(item!!)
        }
    }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.containerView.setOnClickListener(clickListener)
        binding.diffText.setOnClickListener(clickListener)
        binding.userNameText.setOnClickListener {
            if (item != null) {
                callback?.onUserClick(item!!, it)
            }
        }
    }

    fun setItem(item: MwQueryResult.RecentChange) {
        this.item = item
        var isSummaryEmpty = false
        binding.titleText.text = item.title
        binding.summaryText.text = StringUtil.fromHtml(item.parsedcomment).ifEmpty {
            isSummaryEmpty = true
            context.getString(R.string.page_edit_history_comment_placeholder)
        }
        binding.summaryText.setTypeface(Typeface.SANS_SERIF, if (isSummaryEmpty) Typeface.ITALIC else Typeface.NORMAL)
        binding.summaryText.setTextColor(ResourceUtil.getThemedColor(context,
            if (isSummaryEmpty) R.attr.secondary_color else R.attr.primary_color))
        binding.timeText.text = DateUtil.getTimeString(context, item.date)
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
