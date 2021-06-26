package org.wikipedia.feed.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.wikipedia.R
import org.wikipedia.feed.dayheader.DayHeaderMarginItemDecoration
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.AutoFitRecyclerView
import org.wikipedia.views.HeaderMarginItemDecoration
import org.wikipedia.views.MarginItemDecoration

class FeedView constructor(context: Context, attrs: AttributeSet? = null) : AutoFitRecyclerView(context, attrs) {
    private var recyclerLayoutManager: StaggeredGridLayoutManager

    val firstVisibleItemPosition: Int
        get() {
            val manager = layoutManager as StaggeredGridLayoutManager
            val visibleItems = IntArray(manager.spanCount)
            manager.findFirstVisibleItemPositions(visibleItems)
            return visibleItems[0]
        }

    init {
        isVerticalScrollBarEnabled = true
        recyclerLayoutManager = StaggeredGridLayoutManager(columns,
                StaggeredGridLayoutManager.VERTICAL)
        itemAnimator = DefaultItemAnimator()
        layoutManager = recyclerLayoutManager
        addItemDecoration(MarginItemDecoration(context,
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical,
                R.dimen.view_list_card_margin_horizontal, R.dimen.view_list_card_margin_vertical))
        addItemDecoration(HeaderMarginItemDecoration(context,
                R.dimen.view_feed_padding_top, R.dimen.view_feed_search_padding_bottom))
        addItemDecoration(DayHeaderMarginItemDecoration(context, R.dimen.view_feed_day_header_margin_bottom))
        callback = RecyclerViewColumnCallback()
        clipChildren = false
    }

    private inner class RecyclerViewColumnCallback : Callback {
        override fun onColumns(columns: Int) {
            // todo: when there is only one element, should we setSpanCount to 1? e.g.:
            //       adapter.getItemCount() <= 1 ? 1 : columns;
            //       we would need to also notify the layout manager when the data set changes
            //       though.
            recyclerLayoutManager.spanCount = columns
            val padding = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.view_list_card_margin_horizontal))
            updatePadding(left = padding, right = padding)

            // Allow card children to overflow when there's only one column
            clipChildren = columns > 1
        }
    }
}
