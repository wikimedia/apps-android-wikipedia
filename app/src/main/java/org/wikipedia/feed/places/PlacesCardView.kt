package org.wikipedia.feed.places

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewPlacesCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import java.util.Locale

class PlacesCardView(context: Context) : DefaultFeedCardView<PlacesCard>(context) {

    interface Callback {
        fun onGoToPlace(pageTitle: PageTitle? = null, location: Location? = null, enablePermission: Boolean = false)
    }

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
            it.pageTitle.thumbUrl?.let { url ->
                ViewUtil.loadImage(binding.placesCardThumbnail, url, circleShape = true)
            }
            Prefs.placesLastLocationAndZoomLevel?.first?.let { location ->
                if (GeoUtil.isSamePlace(location.latitude, it.location.latitude, location.longitude, it.location.longitude)) {
                    binding.placesCardDistance.text = context.getString(R.string.places_card_distance_unknown)
                } else {
                    val distanceText = GeoUtil.getDistanceWithUnit(location, it.location, Locale.getDefault())
                    binding.placesCardDistance.text = context.getString(R.string.places_card_distance_suffix, distanceText)
                }
            }
            binding.placesCardContainer.setOnClickListener { _ ->
                callback?.onGoToPlace(it.pageTitle, it.location)
            }
            binding.placesCardContainer.setOnLongClickListener { view ->
                LongPressMenu(view, openPageInPlaces = true, location = it.location, callback = object : LongPressMenu.Callback {
                    override fun onOpenInPlaces(entry: HistoryEntry, location: Location) {
                        callback?.onGoToPlace(entry.title, location)
                    }

                    override fun onOpenInNewTab(entry: HistoryEntry) {
                        callback?.onSelectPage(card, entry, true)
                    }

                    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                        callback?.onAddPageToList(entry, addToDefault)
                    }

                    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                        callback?.onMovePageToList(page!!.listId, entry)
                    }
                }).show(HistoryEntry(it.pageTitle, HistoryEntry.SOURCE_FEED_PLACES))
                // TODO: sync with Shay about the HistoryEntry number.

                false
            }
        } ?: run {
            binding.placesEnableLocationContainer.isVisible = true
            binding.placesArticleContainer.isVisible = false
            binding.placesCardContainer.setOnClickListener {
                callback?.onGoToPlace(enablePermission = true)
            }
            binding.placesEnableLocationButton.setOnClickListener {
                callback?.onGoToPlace(enablePermission = true)
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
            callback?.onGoToPlace()
        }
        binding.cardFooter.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode)
    }
}
