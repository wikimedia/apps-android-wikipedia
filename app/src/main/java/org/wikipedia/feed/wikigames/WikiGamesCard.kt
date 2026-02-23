package org.wikipedia.feed.wikigames

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.L10nUtil

class WikiGamesCard(wikiSite: WikiSite) : WikiSiteCard(wikiSite) {
    override fun type(): CardType {
        return CardType.WIKI_GAMES
    }

    fun header(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.on_this_day_game_feed_entry_card_heading)
    }

    override fun title(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.on_this_day_game_feed_entry_card_title)
    }

    override fun subtitle(): String {
        return L10nUtil.getString(wikiSite().languageCode, R.string.on_this_day_game_feed_entry_card_subtitle)
    }

    override fun dismissHashCode(): Int {
        return wikiSite().hashCode() + title().hashCode()
    }
}
