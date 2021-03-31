package org.wikipedia.feed.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.wikipedia.feed.FeedCoordinatorBase
import org.wikipedia.feed.accessibility.AccessibilityCard
import org.wikipedia.feed.announcement.AnnouncementCardView
import org.wikipedia.feed.dayheader.DayHeaderCardView
import org.wikipedia.feed.image.FeaturedImageCardView
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.news.NewsCardView
import org.wikipedia.feed.offline.OfflineCard
import org.wikipedia.feed.offline.OfflineCardView
import org.wikipedia.feed.random.RandomCardView
import org.wikipedia.feed.searchbar.SearchCardView
import org.wikipedia.feed.suggestededits.SuggestedEditsCardView
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder

@Suppress("UNCHECKED_CAST")
class FeedAdapter<T : View>(private val coordinator: FeedCoordinatorBase, private val callback: Callback?) :
    DefaultRecyclerAdapter<Card?, T>(coordinator.cards) {

    interface Callback : ListCardItemView.Callback, CardHeaderView.Callback,
        FeaturedImageCardView.Callback, SearchCardView.Callback, NewsCardView.Callback,
        AnnouncementCardView.Callback, RandomCardView.Callback, ListCardView.Callback,
        SuggestedEditsCardView.Callback {
        fun onShowCard(card: Card?)
        fun onRequestMore()
        fun onRetryFromOffline()
        fun onError(t: Throwable)
    }

    private var feedView: FeedView? = null
    private var lastCardReloadTrigger: Card? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<T> {
        return DefaultViewHolder(newView(parent.context, viewType))
    }

    override fun onBindViewHolder(holder: DefaultViewHolder<T>, position: Int) {
        val item = item(position)
        val view = holder.view as FeedCardView<Card>
        lastCardReloadTrigger = if (coordinator.finished() &&
            position == itemCount - 1 && item !is OfflineCard && item !is AccessibilityCard &&
            item !== lastCardReloadTrigger && callback != null) {
            callback.onRequestMore()
            item
        } else {
            null
        }
        view.card = item
        if (view is OfflineCardView && position == 1) {
            view.setTopPadding()
        }
    }

    override fun onViewAttachedToWindow(holder: DefaultViewHolder<T>) {
        super.onViewAttachedToWindow(holder)
        if (holder.view is SearchCardView) {
            adjustSearchView(holder.view as SearchCardView)
        } else if (holder.view is DayHeaderCardView) {
            adjustDayHeaderView(holder.view as DayHeaderCardView)
        }
        (holder.view as FeedCardView<*>).callback = callback
        callback?.onShowCard((holder.view as FeedCardView<*>).card)
    }

    override fun onViewDetachedFromWindow(holder: DefaultViewHolder<T>) {
        (holder.view as FeedCardView<*>).callback = null
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemViewType(position: Int): Int {
        return item(position)!!.type().code()
    }

    private fun newView(context: Context, viewType: Int): T {
        return CardType.of(viewType).newView(context) as T
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        feedView = recyclerView as FeedView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        feedView = null
    }

    private fun adjustSearchView(view: SearchCardView) {
        view.updateLayoutParams<StaggeredGridLayoutManager.LayoutParams> {
            isFullSpan = true
            bottomMargin = DimenUtil.roundedDpToPx(8F)
            if (DimenUtil.isLandscape(view.context)) {
                leftMargin = (view.parent as View).width / 6
                rightMargin = leftMargin
            }
        }
    }

    private fun adjustDayHeaderView(view: DayHeaderCardView) {
        val layoutParams = view.layoutParams as StaggeredGridLayoutManager.LayoutParams
        layoutParams.isFullSpan = true
    }
}
