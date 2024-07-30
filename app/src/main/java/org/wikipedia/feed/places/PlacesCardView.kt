package org.wikipedia.feed.places

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewPlacesCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.places.PlacesActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import java.util.Locale

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
        card.nearbyPage?.let {
            binding.placesEnableLocationContainer.isVisible = false
            binding.placesArticleContainer.isVisible = true
            binding.placesCardTitle.text = StringUtil.fromHtml(it.pageTitle.displayText)
            binding.placesCardDescription.text = StringUtil.fromHtml(it.pageTitle.description)
            binding.placesCardDescription.isVisible = !it.pageTitle.description.isNullOrEmpty()
            Prefs.placesLastLocationAndZoomLevel?.first?.let { location ->
                val distanceText = GeoUtil.getDistanceWithUnit(location, it.location, Locale.getDefault())
                binding.placesCardDistance.text = context.getString(R.string.places_card_distance_suffix, distanceText)
            }
            binding.placesCardContainer.setOnClickListener { _ ->
                callback?.onSelectPage(card, HistoryEntry(it.pageTitle, HistoryEntry.SOURCE_FEED_FEATURED), false)
            }
        } ?: run {
            binding.placesEnableLocationContainer.isVisible = true
            binding.placesArticleContainer.isVisible = false
            binding.placesCardContainer.setOnClickListener {
                goToPlaces()
            }
            binding.placesEnableLocationButton.setOnClickListener {
                goToPlaces()
            }
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
            goToPlaces()
        }
        binding.cardFooter.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode)
    }

    private fun goToPlaces() {
        context.startActivity(PlacesActivity.newIntent(context))
    }
}
