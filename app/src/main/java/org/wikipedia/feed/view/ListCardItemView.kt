package org.wikipedia.feed.view

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Pair
import org.wikipedia.R
import org.wikipedia.databinding.ViewListCardItemBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageAvailableOfflineHandler.check
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.*
import org.wikipedia.views.ViewUtil
import kotlin.math.roundToInt

class ListCardItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onSelectPage(card: Card, entry: HistoryEntry, openInNewBackgroundTab: Boolean)
        fun onSelectPage(card: Card, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>)
        fun onAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
    }

    private val binding = ViewListCardItemBinding.inflate(LayoutInflater.from(context), this)
    private var card: Card? = null

    @get:VisibleForTesting
    var callback: Callback? = null
        private set

    @get:VisibleForTesting
    var historyEntry: HistoryEntry? = null
        private set

    init {
        isFocusable = true
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val topBottomPadding = 16
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()),
            0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()))
        DeviceUtil.setContextClickAsLongClick(this)
        background = AppCompatResources.getDrawable(getContext(),
            ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground))

        setOnClickListener {
            if (historyEntry != null && card != null) {
                callback?.onSelectPage(card!!, historyEntry!!, TransitionUtil.getSharedElements(context, binding.viewListCardItemImage))
            }
        }

        setOnLongClickListener { view ->
            LongPressMenu(view, true, object : LongPressMenu.Callback {
                override fun onOpenLink(entry: HistoryEntry) {
                    card?.let {
                        callback?.onSelectPage(it, entry, false)
                    }
                }

                override fun onOpenInNewTab(entry: HistoryEntry) {
                    card?.let {
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

    fun setCard(card: Card?): ListCardItemView {
        this.card = card
        return this
    }

    fun setCallback(callback: Callback?): ListCardItemView {
        this.callback = callback
        return this
    }

    fun setHistoryEntry(entry: HistoryEntry): ListCardItemView {
        historyEntry = entry
        setTitle(StringUtil.fromHtml(entry.title.displayText))
        setSubtitle(entry.title.description)
        setImage(entry.title.thumbUrl)
        check(entry.title) { available -> setViewsGreyedOut(!available) }
        return this
    }

    @VisibleForTesting
    fun setImage(url: String?) {
        if (url == null) {
            binding.viewListCardItemImage.visibility = GONE
        } else {
            binding.viewListCardItemImage.visibility = VISIBLE
            ViewUtil.loadImageWithRoundedCorners(binding.viewListCardItemImage, url, true)
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

    fun setNumber(number: Int) {
        binding.viewListCardNumber.visibility = VISIBLE
        binding.viewListCardNumber.setNumber(number)
    }

    fun setPageViews(pageViews: Long) {
        binding.viewListCardItemPageviews.visibility = VISIBLE
        binding.viewListCardItemPageviews.text = getPageViewText(pageViews)
    }

    fun setGraphView(viewHistories: List<PageSummary.ViewHistory>) {
        val dataSet = mutableListOf<Float>()
        var i = viewHistories.size
        while (DEFAULT_VIEW_HISTORY_ITEMS > i++) {
            dataSet.add(0f)
        }
        dataSet.addAll(viewHistories.map { it.views })
        binding.viewListCardItemGraph.visibility = VISIBLE
        binding.viewListCardItemGraph.setData(dataSet)
    }

    private fun getPageViewText(pageViews: Long): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val primaryLocale = context.resources.configuration.locales[0]
            val decimalFormat = CompactDecimalFormat.getInstance(primaryLocale, CompactDecimalFormat.CompactStyle.SHORT)
            return decimalFormat.format(pageViews)
        }
        return when {
            pageViews < 1000 -> pageViews.toString()
            pageViews < 1000000 -> {
                context.getString(
                        R.string.view_top_read_card_pageviews_k_suffix,
                        (pageViews / 1000f).roundToInt()
                )
            }
            else -> {
                context.getString(
                        R.string.view_top_read_card_pageviews_m_suffix,
                        (pageViews / 1000000f).roundToInt()
                )
            }
        }
    }

    private fun setViewsGreyedOut(greyedOut: Boolean) {
        val alpha = if (greyedOut) 0.5f else 1.0f
        binding.viewListCardItemTitle.alpha = alpha
        binding.viewListCardItemSubtitle.alpha = alpha
        binding.viewListCardItemImage.alpha = alpha
    }

    companion object {
        private const val DEFAULT_VIEW_HISTORY_ITEMS = 5
    }
}
