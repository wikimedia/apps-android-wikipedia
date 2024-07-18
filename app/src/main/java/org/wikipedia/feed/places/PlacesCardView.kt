package org.wikipedia.feed.places

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import org.wikipedia.databinding.ViewPlacesCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.places.PlacesActivity
import org.wikipedia.settings.Prefs

class PlacesCardView(context: Context) : DefaultFeedCardView<PlacesCard>(context) {

    private val binding = ViewPlacesCardBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: PlacesCard? = null
        set(value) {
            field = value
            value?.let {
                header(it)
                footer(it)
                updateContents(it)
            }
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.cardHeader.setCallback(value)
        }

    private fun updateContents(card: PlacesCard) {
        if (Prefs.placesLastLocationAndZoomLevel == null) {
            binding.placesEnableLocationContainer.isVisible = true
            binding.placesCardContainer.isVisible = false
        } else {
            binding.placesEnableLocationContainer.isVisible = false
            binding.placesCardContainer.isVisible = true
        }
    }

    private fun header(card: PlacesCard) {
        binding.cardHeader.setTitle(card.title())
            .setCard(card)
            .setLangCode(null)
            .setCallback(callback)
    }

    private fun footer(card: PlacesCard) {
        binding.cardFooter.callback = CardFooterView.Callback {
            context.startActivity(PlacesActivity.newIntent(context))
        }
        binding.cardFooter.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode)
    }
}
