package org.wikipedia.feed.wikigames

import android.content.Context
import android.view.LayoutInflater
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.ViewWikiGamesCardBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil

class WikiGamesCardView(context: Context) : DefaultFeedCardView<WikiGamesCard>(context) {
    private val binding = ViewWikiGamesCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.viewWikiGamesCardContentContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.progressive_color))
        binding.viewWikiGamesCardContentContainer.setOnClickListener {
            context.startActivity(OnThisDayGameActivity.newIntent(context, Constants.InvokeSource.FEED))
        }
    }

    override var card: WikiGamesCard? = null
        set(value) {
            field = value
            value?.let {
                val langCode = it.wikiSite.languageCode
                setHeader(langCode, it)
                setTitle(langCode)
                setSubTitle(langCode)
            }
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.viewWikiGamesCardHeader.setCallback(value)
        }

    private fun setHeader(langCode: String, card: WikiGamesCard) {
        binding.viewWikiGamesCardHeader
            .setTitle(L10nUtil.getStringForArticleLanguage(langCode, R.string.on_this_day_game_feed_entry_card_heading))
            .setLangCode(langCode)
            .setCard(card)
    }

    private fun setTitle(langCode: String) {
        binding.viewWikiGamesCardTitle.setText(L10nUtil.getStringForArticleLanguage(langCode, R.string.on_this_day_game_feed_entry_card_title))
    }

    private fun setSubTitle(langCode: String) {
        binding.viewWikiGamesCardSubTitle.setText(L10nUtil.getStringForArticleLanguage(langCode, R.string.on_this_day_game_feed_entry_card_subtitle))
    }
}
