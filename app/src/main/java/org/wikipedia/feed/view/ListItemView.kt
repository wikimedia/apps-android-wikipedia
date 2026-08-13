package org.wikipedia.feed.view

import android.content.Context
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.databinding.ViewListItemBinding
import org.wikipedia.extensions.coroutineScope
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageAvailableOfflineHandler
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil
import org.wikipedia.views.ViewUtil

class ListItemView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onSelectPage(title: PageTitle, entry: HistoryEntry, openInNewBackgroundTab: Boolean)
        fun onSelectPage(title: PageTitle, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>)
        fun onAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
    }

    private val binding = ViewListItemBinding.inflate(LayoutInflater.from(context), this)
    private var title: PageTitle? = null

    @get:VisibleForTesting
    var callback: Callback? = null
        private set

    @get:VisibleForTesting
    var historyEntry: HistoryEntry? = null
        private set

    init {
        isFocusable = true
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val topBottomPadding = 16
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()),
            0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()))
        DeviceUtil.setContextClickAsLongClick(this)
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackground))

        setOnClickListener {
            if (historyEntry != null && title != null) {
                callback?.onSelectPage(title!!, historyEntry!!, TransitionUtil.getSharedElements(context, binding.viewListCardItemImage))
            }
        }

        setOnLongClickListener { view ->
            LongPressMenu(view, callback = object : LongPressMenu.Callback {
                override fun onOpenLink(entry: HistoryEntry) {
                    title?.let {
                        callback?.onSelectPage(it, entry, false)
                    }
                }

                override fun onOpenInNewTab(entry: HistoryEntry) {
                    title?.let {
                        callback?.onSelectPage(it, entry, true)
                    }
                }

                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    callback?.onAddPageToList(entry, addToDefault)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        callback?.onMovePageToList(it.listId, entry)
                    }
                }
            }).show(historyEntry)
            false
        }
    }

    fun setPageTitle(title: PageTitle?): ListItemView {
        this.title = title
        return this
    }

    fun setCallback(callback: Callback?): ListItemView {
        this.callback = callback
        return this
    }

    fun setHistoryEntry(entry: HistoryEntry): ListItemView {
        historyEntry = entry
        setTitle(StringUtil.fromHtml(entry.title.displayText))
        setSubtitle(entry.title.description)
        setImage(entry.title.thumbUrl)
        PageAvailableOfflineHandler.check(coroutineScope(), entry.title) { setViewsGreyedOut(!it) }
        return this
    }

    @VisibleForTesting
    fun setImage(url: String?) {
        if (url == null) {
            binding.viewListCardItemImage.visibility = GONE
        } else {
            binding.viewListCardItemImage.visibility = VISIBLE
            ViewUtil.loadImage(binding.viewListCardItemImage, url)
        }
    }

    @VisibleForTesting
    fun setTitle(text: CharSequence?) {
        binding.viewListCardItemTitle.text = text
    }

    @VisibleForTesting
    fun setSubtitle(text: CharSequence?) {
        binding.viewListCardItemSubtitle.text = text
    }

    private fun setViewsGreyedOut(greyedOut: Boolean) {
        val alpha = if (greyedOut) 0.5f else 1.0f
        binding.viewListCardItemTitle.alpha = alpha
        binding.viewListCardItemSubtitle.alpha = alpha
        binding.viewListCardItemImage.alpha = alpha
    }
}
