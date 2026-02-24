package org.wikipedia.feed.wikigames

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.ViewWikiGamesCardBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.util.ResourceUtil

class WikiGamesCardView(context: Context) : DefaultFeedCardView<WikiGamesCard>(context) {
    private val binding = ViewWikiGamesCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.viewWikiGamesCardContentContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.progressive_color))
        binding.viewWikiGamesCardContentContainer.setOnClickListener {
            WikiGamesEvent.submit("enter_click", "game_feed")
            (context as? Activity)?.startActivityForResult(OnThisDayGameActivity.newIntent(context, Constants.InvokeSource.FEED, card!!.wikiSite()), 0)
        }
    }

    override var card: WikiGamesCard? = null
        set(value) {
            field = value
            value?.let {
                setHeader(it)
                setTitle(it)
                setSubTitle(it)
            }
            WikiGamesEvent.submit("impression", "game_feed")
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.viewWikiGamesCardHeader.setCallback(value)
        }

    private fun setHeader(card: WikiGamesCard) {
        binding.viewWikiGamesCardHeader
            .setTitle(card.header())
            .setLangCode(card.wikiSite().languageCode)
            .setCard(card)
    }

    private fun setTitle(card: WikiGamesCard) {
        binding.viewWikiGamesCardTitle.text = card.title()
    }

    private fun setSubTitle(card: WikiGamesCard) {
        binding.viewWikiGamesCardSubTitle.text = card.subtitle()
    }
}
