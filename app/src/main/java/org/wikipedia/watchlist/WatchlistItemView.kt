package org.wikipedia.watchlist

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.dataclient.mwapi.MwQueryResult
import kotlinx.android.synthetic.main.item_watchlist.view.*
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class WatchlistItemView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    var callback: Callback? = null
    private var item: MwQueryResult.WatchlistItem? = null
    private var clickListener = OnClickListener {
        if (item != null) {
            callback?.onItemClick(item!!)
        }
    }

    init {
        View.inflate(context, R.layout.item_watchlist, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        containerView.setOnClickListener(clickListener)
        diffText.setOnClickListener(clickListener)
        userNameText.setOnClickListener {
            if (item != null) {
                callback?.onUserClick(item!!)
            }
        }
    }

    fun setItem(item: MwQueryResult.WatchlistItem) {
        this.item = item
        titleText.text = item.title
        langCodeText.text = item.wiki.languageCode()
        summaryText.text = StringUtil.fromHtml(item.parsedComment)
        timeText.text = DateUtil.getTimeString(item.date)
        userNameText.text = item.user

        userNameText.setIconResource(if (item.isAnon) R.drawable.ic_anonymous_ooui else R.drawable.ic_baseline_person_24)

        val diffByteCount = item.newlen - item.oldlen
        if (diffByteCount >= 0) {
            diffText.setTextColor(if (diffByteCount > 0) ContextCompat.getColor(context, R.color.green50) else ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
            diffText.text = String.format("%+d", diffByteCount)
        } else {
            diffText.setTextColor(ContextCompat.getColor(context, R.color.red50))
            diffText.text = String.format("%+d", diffByteCount)
        }
    }

    interface Callback {
        fun onItemClick(item: MwQueryResult.WatchlistItem)
        fun onUserClick(item: MwQueryResult.WatchlistItem)
    }
}