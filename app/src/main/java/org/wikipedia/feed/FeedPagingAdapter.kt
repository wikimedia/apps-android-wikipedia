package org.wikipedia.feed

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class FeedPagingAdapter : PagingDataAdapter<Card, RecyclerView.ViewHolder>(CARD_DIFF) {

    var callback: FeedAdapter.Callback? = null

    companion object {
        private val CARD_DIFF = object : DiffUtil.ItemCallback<Card>() {
            override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val card = getItem(position) ?: return -1
        return card.type().code()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return FeedCardViewHolder(CardType.of(viewType).newView(parent.context) as View)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val card = getItem(position) ?: return
        val view = holder.itemView
        @Suppress("UNCHECKED_CAST")
        (view as? FeedCardView<Card>)?.let { feedView ->
            feedView.card = card
            feedView.callback = callback
        }
    }

    private class FeedCardViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
