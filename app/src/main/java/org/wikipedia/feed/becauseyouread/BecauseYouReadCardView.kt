package org.wikipedia.feed.becauseyouread

import android.content.Context
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.feed.view.ListCardRecyclerAdapter
import org.wikipedia.feed.view.ListCardView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.views.DefaultViewHolder

class BecauseYouReadCardView(context: Context) : ListCardView<BecauseYouReadCard>(context) {
    override var card: BecauseYouReadCard? = null
        set(value) {
            field = value
            value?.let {
                header(it)
                set(RecyclerAdapter(it.items()))
                setLayoutDirectionByWikiSite(it.wikiSite(), layoutDirectionView)
            }
        }

    private fun header(card: BecauseYouReadCard) {
        headerView.setTitle(card.title())
            .setLangCode(card.wikiSite().languageCode())
            .setCard(card)
            .setCallback(callback)

        largeHeaderView.setTitle(card.pageTitle())
            .setLanguageCode(card.wikiSite().languageCode())
            .setImage(card.image())
            .setSubtitle(card.extract())

        largeHeaderContainer.visibility = VISIBLE

        largeHeaderContainer.setOnClickListener {
            callback?.onSelectPage(card, HistoryEntry(card.pageTitle,
                HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ), largeHeaderView.sharedElements)
        }
    }

    private inner class RecyclerAdapter constructor(items: List<BecauseYouReadItemCard>) :
        ListCardRecyclerAdapter<BecauseYouReadItemCard>(items) {

        override fun callback(): ListCardItemView.Callback? {
            return callback
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<ListCardItemView>, i: Int) {
            val card = item(i)
            holder.view.setCard(card)
                .setHistoryEntry(HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ))
        }
    }
}
