package org.wikipedia.watchlist

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
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemWatchlistBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class WatchlistItemView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    val binding = ItemWatchlistBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: Callback? = null
    private var item: MwQueryResult.WatchlistItem? = null
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
        if (WikipediaApp.instance.languageState.appLanguageCodes.size == 1) {
            binding.langCodeText.visibility = GONE
        } else {
            binding.langCodeText.visibility = VISIBLE
        }
    }

    fun setItem(item: MwQueryResult.WatchlistItem, currentQuery: String?) {
        this.item = item
        var isSummaryEmpty = false
        binding.langCodeText.setLangCode(item.wiki!!.languageCode)
        var summary = StringUtil.fromHtml(item.parsedComment).ifEmpty {
            isSummaryEmpty = true
            context.getString(R.string.page_edit_history_comment_placeholder)
        }
        binding.summaryText.setTypeface(Typeface.SANS_SERIF, if (isSummaryEmpty) Typeface.ITALIC else Typeface.NORMAL)
        binding.summaryText.setTextColor(ResourceUtil.getThemedColor(context,
            if (isSummaryEmpty) R.attr.secondary_color else R.attr.primary_color))
        binding.timeText.text = DateUtil.getTimeString(context, item.date)
        binding.userNameText.contentDescription = context.getString(R.string.talk_user_title, item.user)

        binding.userNameText.setIconResource(if (item.isAnon) R.drawable.ic_anonymous_ooui else R.drawable.ic_user_avatar)
        if (item.logtype.isNotEmpty()) {
            binding.diffText.isVisible = true
            when (item.logtype) {
                context.getString(R.string.page_moved) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_moved), R.drawable.ic_info_outline_black_24dp)
                }
                context.getString(R.string.page_protected) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_protected), R.drawable.ic_baseline_lock_24)
                }
                context.getString(R.string.page_deleted) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_deleted), R.drawable.ic_delete_white_24dp)
                }
                else -> {
                    binding.diffText.isVisible = false
                    summary = StringUtil.fromHtml(item.logdisplay)
                }
            }
            binding.containerView.alpha = 0.5f
            binding.containerView.isClickable = false
        } else {
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
        L10nUtil.setConditionalLayoutDirection(this, item.wiki!!.languageCode)
        StringUtil.setHighlightedAndBoldenedText(binding.titleText, item.title, currentQuery)
        StringUtil.setHighlightedAndBoldenedText(binding.userNameText, item.user, currentQuery)
        StringUtil.setHighlightedAndBoldenedText(binding.summaryText, summary, currentQuery)
    }

    private fun setButtonTextAndIconColor(text: String, @DrawableRes iconResourceDrawable: Int = 0) {
        val themedTint = ResourceUtil.getThemedColorStateList(context, R.attr.border_color)
        binding.diffText.text = text
        binding.diffText.setTextColor(themedTint)
        binding.diffText.setIconResource(iconResourceDrawable)
        binding.diffText.iconTint = themedTint
    }

    interface Callback {
        fun onItemClick(item: MwQueryResult.WatchlistItem)
        fun onUserClick(item: MwQueryResult.WatchlistItem, view: View)
    }
}
