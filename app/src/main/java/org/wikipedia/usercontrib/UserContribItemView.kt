package org.wikipedia.usercontrib

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemUserContribBinding
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class UserContribItemView(context: Context) : FrameLayout(context) {
    interface Listener {
        fun onClick()
    }

    var listener: Listener? = null
    private val binding = ItemUserContribBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.clickTargetView.setOnClickListener {
            listener?.onClick()
        }
    }

    fun setContents(contrib: UserContribution, currentQuery: String?) {
        binding.diffText.text = StringUtil.getDiffBytesText(context, contrib.sizediff)
        if (contrib.sizediff >= 0) {
            val diffColor = if (contrib.sizediff > 0) R.attr.success_color else R.attr.secondary_color
            binding.diffText.setTextColor(ResourceUtil.getThemedColor(context, diffColor))
        } else {
            binding.diffText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.destructive_color))
        }
        binding.articleTitle.text = StringUtil.fromHtml(contrib.title)

        if (contrib.comment.isEmpty()) {
            binding.editSummary.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            binding.editSummary.setTextColor(ResourceUtil.getThemedColor(context, R.attr.secondary_color))
            binding.editSummary.text = context.getString(R.string.page_edit_history_comment_placeholder)
        } else {
            binding.editSummary.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            binding.editSummary.setTextColor(ResourceUtil.getThemedColor(context, R.attr.primary_color))
            binding.editSummary.text = if (contrib.minor) StringUtil.fromHtml(context.getString(R.string.page_edit_history_minor_edit, contrib.comment)) else contrib.comment
            StringUtil.highlightAndBoldenText(binding.editSummary, currentQuery, true, Color.YELLOW)
        }
        binding.currentIndicator.isVisible = contrib.top
        binding.editHistoryTimeText.text = DateUtil.getTimeString(context, contrib.localDateTime)
        StringUtil.highlightAndBoldenText(binding.diffText, currentQuery, true, Color.YELLOW)
        StringUtil.highlightAndBoldenText(binding.articleTitle, currentQuery, true, Color.YELLOW)
    }
}
