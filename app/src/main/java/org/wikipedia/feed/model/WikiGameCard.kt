package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.feed.wikigames.WikiGame
@Serializable
class WikiGameCard(
    val wikiGame: WikiGame,
    val date: String
) : ForYouCard() {
    override fun dismissHashCode(): Int = wikiGame.game.name.hashCode() + date.hashCode()
}
