package org.wikipedia.feed.wikigames

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.L10nUtil

class WikiGamesCard(val wikiSite: WikiSite, val games: List<WikiGame>) : Card() {
    override fun type(): CardType {
        return CardType.WIKI_GAMES
    }

    override fun dismissHashCode(): Int {
        return wikiSite.hashCode()
    }
}
