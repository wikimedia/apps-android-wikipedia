package org.wikipedia.watchlist

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
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
                callback?.onUserClick(item!!)
            }
        }
        if (WikipediaApp.getInstance().language().appLanguageCodes.size == 1) {
            binding.langCodeBackground.visibility = GONE
            binding.langCodeText.visibility = GONE
        } else {
            binding.langCodeBackground.visibility = VISIBLE
            binding.langCodeText.visibility = VISIBLE
        }
    }

    fun setItem(item: MwQueryResult.WatchlistItem) {
        this.item = item
        binding.titleText.text = item.title
        binding.langCodeText.text = item.wiki.languageCode
        binding.summaryText.text = StringUtil.fromHtml(item.parsedComment)
        binding.timeText.text = DateUtil.getTimeString(item.date)
        binding.userNameText.text = item.user

        binding.userNameText.setIconResource(if (item.isAnon) R.drawable.ic_anonymous_ooui else R.drawable.ic_user_talk)
        if (item.logType.isNotEmpty()) {
            when (item.logType) {
                context.getString(R.string.page_moved) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_moved), R.attr.suggestions_background_color, R.drawable.ic_info_outline_black_24dp)
                }
                context.getString(R.string.page_protected) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_protected), R.attr.suggestions_background_color, R.drawable.ic_baseline_lock_24)
                }
                context.getString(R.string.page_deleted) -> {
                    setButtonTextAndIconColor(context.getString(R.string.watchlist_page_deleted), R.attr.suggestions_background_color, R.drawable.ic_delete_white_24dp)
                }
            }
            binding.containerView.alpha = 0.5f
            binding.containerView.isClickable = false
        } else {
            val diffByteCount = item.newlen - item.oldlen
            setButtonTextAndIconColor(String.format(if (diffByteCount != 0) "%+d" else "%d", diffByteCount), R.attr.color_group_22)
            if (diffByteCount >= 0) {
                binding.diffText.setTextColor(if (diffByteCount > 0) ContextCompat.getColor(context, R.color.green50)
                else ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
            } else {
                binding.diffText.setTextColor(ContextCompat.getColor(context, R.color.red50))
            }
            binding.containerView.alpha = 1.0f
            binding.containerView.isClickable = true
        }
        L10nUtil.setConditionalLayoutDirection(this, item.wiki.languageCode)
    }

    private fun setButtonTextAndIconColor(text: String, @AttrRes backgroundTint: Int, @DrawableRes iconResourceDrawable: Int? = null) {
        val themedTint = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.color_group_61))
        binding.diffText.text = text
        binding.diffText.setTextColor(themedTint)
        binding.diffText.icon = if (iconResourceDrawable == null) null
        else ContextCompat.getDrawable(context, iconResourceDrawable)
        binding.diffText.iconTint = themedTint
        binding.diffText.setBackgroundColor(ResourceUtil.getThemedColor(context, backgroundTint))
    }

    interface Callback {
        fun onItemClick(item: MwQueryResult.WatchlistItem)
        fun onUserClick(item: MwQueryResult.WatchlistItem)
    }
}
