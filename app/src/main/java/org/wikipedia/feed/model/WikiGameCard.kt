package org.wikipedia.feed.model

import org.wikipedia.feed.wikigames.WikiGame

class WikiGameCard(
    val wikiGame: WikiGame,
    val date: String
) : ForYouCard() {
    override fun dismissHashCode(): Int = wikiGame.game.name.hashCode() + date.hashCode()
}
