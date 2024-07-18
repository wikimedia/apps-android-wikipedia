package org.wikipedia.feed.places

import android.content.Context
import android.view.LayoutInflater
import org.wikipedia.databinding.ViewSuggestedEditsCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter

class PlacesCardView(context: Context) : DefaultFeedCardView<PlacesCard>(context), CardFooterView.Callback {

    private val binding = ViewSuggestedEditsCardBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: PlacesCard? = null
        set(value) {
            field = value
            value?.let {
                header(it)
                updateContents(it)
            }
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.headerView.setCallback(value)
        }

    override fun onFooterClicked() {
        callback?.onSeCardFooterClicked()
    }

    private fun updateContents(card: PlacesCard) {
        // TODO
    }

    private fun header(card: PlacesCard) {
        binding.headerView.setTitle(card.title())
            .setCard(card)
            .setLangCode(null)
            .setCallback(callback)
    }
}
